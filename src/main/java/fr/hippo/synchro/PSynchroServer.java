package fr.hippo.synchro;

import com.jcraft.jsch.*;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Vector;

public class PSynchroServer {
	private static ArrayList<String> alreadyDownloaded = new ArrayList<>();

    private Path configFile;
    private Config config;

	final static Logger LOG = Logger.getLogger(PSynchroServer.class);

	public static void main(String[] args) {

		PropertyConfigurator.configure("log4j.properties");

		LOG.info("Start program PSynchroServer");
		PSynchroServer program = new PSynchroServer();
		program.run(args);
	}

	private void run(String[] args) {
		boolean stop = false;

		Path stopFile = Paths.get("stop.stop");
		configFile = Paths.get("reload.config");

        config = new Config();
        if (!loadConfig()) {
            LOG.error("cannot read config, exit the program");
            return;
        }

		while (!stop) {

			int nbSecToSleep = doLoop();

			LOG.info("sleep for " + nbSecToSleep + " s");

			try {
				Thread.sleep(nbSecToSleep * 1000);
			} catch (InterruptedException e) {
				LOG.error("cannot sleep");
			}

			if (Files.exists(stopFile)) {
				LOG.info("stop file has been created");
				LOG.info("it will end the program");
				try {
					Files.delete(stopFile);
				} catch (IOException e) {
					LOG.error("cannot delete stop file");
				}
				stop = true;
			}

		}

		LOG.info("stop the program");
	}

	private int doLoop() {
		Session session = null;
		Channel channel = null;
		JSch ssh = new JSch();

        if (Files.exists(configFile)) {
            LOG.info("reload properties");
            if (loadConfig()) {
                try {
                    Files.delete(configFile);
                } catch (IOException e) {
                    LOG.error("cannot delete config file");
                }
            } else {
                return Integer.parseInt(Config.SECOND_TO_SLEEP);
            }
        }

		List<String> foldersToDowload;

		try {
            foldersToDowload = Files.readAllLines(Paths.get(config.getWantedFile()));
		} catch (IOException e) {
			LOG.error("cannot get wanted files from " + config.getWantedFile());
			LOG.error(e.getMessage());
			LOG.error(e);
			return Integer.parseInt(Config.SECOND_TO_SLEEP);
		}

		ArrayList<String> downloaded = new ArrayList<>();

		getAlreadyDownloaded();

		// ssh.setKnownHosts("setKnownHosts");
		try {

			session = ssh.getSession(config.getUsername(), config.getServer(), 2222);

			Properties configProperties = new Properties();
			configProperties.put("StrictHostKeyChecking", "no");
			session.setConfig(configProperties);

			session.setPassword(config.getPassword());
			session.connect();
			channel = session.openChannel("sftp");
			channel.connect();
			ChannelSftp sftp = (ChannelSftp) channel;

			String s;

			for (String folder : foldersToDowload) {
				// System.out.println("[SERVER] try to reach " + folder + s);

				s = Normalizer.normalize(folder, Normalizer.Form.NFD);

				s = s.replaceAll("[^\\p{ASCII}]", "");

				if (downloaded.size() >= config.getNbDL()) {
					continue;
				}

				if (s == null || s.startsWith("#")) {
					continue;
				}

                String path = config.getFromFolder() + s;
				try {
                    sftp.cd(path);
				} catch (SftpException sftpe) {
					LOG.error("[SERVER] cannot go to " + path);
					continue;
				}

				Vector<ChannelSftp.LsEntry> ls = sftp.ls("*");

				// System.out.println(ls.size() + " files in this folder");

                String tempPathString = config.getTempFolder() + s;
                Path tempPath = Paths.get(tempPathString);
                String destPathString = config.getDestFolder() + s;
                Path destPath = Paths.get(destPathString);

				Path tempFilePath;
				Path destFilePath;

				// System.out.println("[CLIENT] try to reach " + tempPath);

				try {
					Files.createDirectories(tempPath);
				} catch (IOException e) {
					LOG.error("[CLIENT] cannot create temp folder " + tempPath);
					continue;
				}

				try {
					Files.createDirectories(destPath);
				} catch (IOException e) {
					LOG.error("[CLIENT] cannot create dest folder " + destPath);
					continue;
				}

				try {
					sftp.lcd(tempPathString);
				} catch (SftpException sftpe) {
					LOG.error(tempPathString + " is not a correct folder");
					continue;
				}
				String fileName;
				for (ChannelSftp.LsEntry entry : ls) {

					if (downloaded.size() >= config.getNbDL()) {
						continue;
					}

					fileName = entry.getFilename();

					fileName = Normalizer.normalize(fileName, Normalizer.Form.NFD);

					fileName = fileName.replaceAll("[^\\p{ASCII}]", "");

					if (alreadyDownloaded.contains(s + "/" + fileName)) {
						// System.out.println(fileName + " already downloaded");
					} else {
						LOG.info("download " + fileName);

						if (isLimitReached(config.getDiskName(), config.getDiskLimit(), entry.getAttrs().getSize())) {
							LOG.error("limit reached");
							continue;
						}

						SPMImpl monitor = new SPMImpl();
						monitor.init(SPMImpl.GET, fileName, fileName, entry.getAttrs().getSize());

						sftp.get(entry.getFilename(), fileName, monitor);

						LOG.info(fileName + " downloaded");

						tempFilePath = Paths.get(tempPathString + "/" + fileName);
						destFilePath = Paths.get(destPathString + "/" + fileName);

						try {
							if (!tempFilePath.equals(destFilePath)) {
                                LOG.info("move to final folder");
								Files.move(tempFilePath, destFilePath);
							}

                            int speed = monitor.getSpeed(monitor.size, monitor.startTime, monitor.endTime);
                            long totalSecs = monitor.endTime - monitor.startTime;
                            long totalSecsMove = System.currentTimeMillis() - monitor.endTime;
                            int speedMove = monitor.getSpeed(monitor.size, monitor.endTime, System.currentTimeMillis());
                            String text = getText(fileName, monitor.size, totalSecs, speed, totalSecsMove, speedMove);

                            switch (config.getMessageType()) {
                                case EMAIL:
							        sendMail(fileName, text);
                                    break;
                                case TELEGRAM:
							        sendTelegram(text);
                                    break;
                                case PUSH_BULLET:
                                    //TODO
                                    break;
                            }
							downloaded.add(s + "/" + fileName);
						} catch (IOException e) {
							System.err.println("cannot move " + tempFilePath + " to " + destFilePath);
							System.err.println(e.toString());
						}

						if (Files.exists(Paths.get("stop.stop"))) {
							System.out.println("stop file has been created while downloading");
							config.setNbDL(0);
						}

					}
				}

				// System.out.println("all episodes of " + s + " downloaded");
			}

		} catch (JSchException e) {
			e.printStackTrace();
		} catch (SftpException e) {
			e.printStackTrace();
		} finally {
			if (channel != null) {
				channel.disconnect();
			}
			if (session != null) {
				session.disconnect();
			}
		}

		if (!downloaded.isEmpty()) {
			LOG.info(downloaded.size() + " files downloaded");
		}

		write(downloaded);

		return config.getSecondToSleep();
	}

    private boolean loadConfig() {
        try {
            config.initFromProperties(getPropValues());
            LOG.info("new properties = " + config);
        } catch (IOException e1) {
            LOG.error("cannot get properties");
            return false;
        }
        return true;
    }

    public Properties getPropValues() throws IOException {

		Properties prop = new Properties();
		String propFileName = "config.properties";

		InputStream inputStream = new FileInputStream(propFileName);

		prop.load(inputStream);

        inputStream.close();

		return prop;
	}

	private static void getAlreadyDownloaded() {

		File f = new File("downloaded_files.txt");
		if (!f.exists()) {
			try {
				LOG.info("create file " + f.getName());
				f.createNewFile();
			} catch (IOException ex) {
				LOG.error("cannot create file " + f.getName());
			}
		} else {
			try {
				alreadyDownloaded.clear();
				alreadyDownloaded.addAll(Files.readAllLines(Paths.get("downloaded_files.txt"),
						Charset.forName("ISO-8859-1")));
			} catch (IOException ex) {
				LOG.error("cannot read file " + f.getName());
			}
		}
	}

	private void write(ArrayList<String> downloaded) {
		FileWriter writer;
		try {

			writer = new FileWriter("downloaded_files.txt", true);

			for (String s : downloaded) {
				writer.write(s + System.lineSeparator());
			}

			writer.close();

		} catch (IOException ex) {
			LOG.error("io exception", ex);
		}
	}

	public boolean isLimitReached(String name, String limit, long size) {
		LOG.info("size: " + (int) (size / 1024) + "ko | free: "
				+ (int) (new File(name).getFreeSpace() / 1024) + "ko");
		return new File(name).getFreeSpace() < Long.parseLong(limit) + size;
	}

	private static String getText(String title, long size, long totalSecs, int speed, long totalSecsMove, int speedMove) {
		String sizeTexte = (int) (size / (1024 * 1024)) + " Mo";

		totalSecs = totalSecs / 1000;

		int hours = (int) totalSecs / 3600;
		int minutes = (int) (totalSecs % 3600) / 60;
		int seconds = (int) totalSecs % 60;

		String elapsedTime = String.format("%02d:%02d:%02d", hours, minutes, seconds);

		totalSecsMove = totalSecsMove / 1000;

		hours = (int) totalSecsMove / 3600;
		minutes = (int) (totalSecsMove % 3600) / 60;
		seconds = (int) totalSecsMove % 60;

		String elapsedTimeMove = String.format("%02d:%02d:%02d", hours, minutes, seconds);

		return title + "\n" + sizeTexte + "\ndownload: " + elapsedTime + " (" + speed
				+ "ko/s)" + "\nmove: " + elapsedTimeMove + " (" + speedMove + "ko/s)";
	}

	public boolean sendTelegram(String text) {
        URL url;
        try {
            String urlText = "https://api.telegram.org/" + config.getMessageFrom()
                    + "/sendMessage?chat_id=" + config.getMessageDest() + "&text=" + URLEncoder.encode(text, "UTF-8");
			LOG.debug(urlText);
            url = new URL(urlText);
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
			int responseCode = urlConnection.getResponseCode();
			urlConnection.disconnect();
			if (responseCode != 200) {
				LOG.error("wrong status url telegram : " + responseCode);
				return false;
			}
			urlConnection.disconnect();
        } catch (MalformedURLException e) {
            LOG.error("malformed url telegram", e);
            return false;
        } catch (IOException e) {
            LOG.error("fail during telegram curl", e);
            return false;
        }

        return true;

    }

	public boolean sendMail(String title, String text) {

		String host = "smtp.free.fr";

		Properties properties = System.getProperties();

		properties.setProperty("mail.smtp.host", host);

		javax.mail.Session session = javax.mail.Session.getDefaultInstance(properties);

		try {
			MimeMessage message = new MimeMessage(session);

			// Set From: header field of the header.
			message.setFrom(new InternetAddress(config.getMessageFrom()));

			// Set To: header field of the header.
			message.addRecipient(Message.RecipientType.TO, new InternetAddress(config.getMessageDest()));

			// Set Subject: header field
			message.setSubject(title + " downloaded");

			message.setText(text);

			// Send message
			Transport.send(message);
			System.out.println("Sent message successfully....");
		} catch (MessagingException mex) {
			LOG.error(mex);
			return false;
		}
		return true;
	}

}
