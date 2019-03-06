package org.daisy.dotify.translator.impl.liblouis;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.daisy.dotify.common.io.InterProcessLock;

public enum LiblouisTables {
	INSTANCE;
	
	private final Logger logger = Logger.getLogger(LiblouisTables.class.getCanonicalName());
	private final int TABLES_VERSION = 1;
	private final List<String> tables;
	private final Path liblouisTablesPath;
	
	private LiblouisTables() {
		this.tables = getTables();
		Path userHome = Paths.get(System.getProperty("user.home"));
		Path liblouisPath = userHome.resolve(".dotify").resolve("liblouis");
		this.liblouisTablesPath = liblouisPath.resolve("tables");
		try {
			Files.createDirectories(liblouisTablesPath);
		} catch (IOException e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
			return;
		}
		Path lockPath = liblouisPath.resolve(".lock");
		Path updated = liblouisPath.resolve("version");
		InterProcessLock lock = new InterProcessLock(lockPath.toFile());
		if (lock.lock()) {
			try {
				if (shouldUpdateTablesCache(updated)) {
					updateTablesCache();
					try {
						Files.write(updated, Arrays.asList(TABLES_VERSION+""));
					} catch (IOException e) {
						if (logger.isLoggable(Level.FINE)) {
							logger.log(Level.FINE, "Failed to update version file.", e);
						}
					}
				} else if (logger.isLoggable(Level.FINE)) {
					logger.fine("Tables are up to date");
				}
			} finally {
				lock.unlock();
			}
		} else {
			logger.warning("Failed to update table cache.");
		}
	}
	
	boolean shouldUpdateTablesCache(Path updated) {
		if (!Files.exists(updated)) {
			return true;
		} else {
			 try {
				List<String> lines = Files.readAllLines(updated);
				if (lines.isEmpty()) {
					return true;
				}
				String line = lines.get(0);
				int v = Integer.parseInt(line);
				return (v!=TABLES_VERSION);
			} catch (IOException | NumberFormatException e) {
				return true;
			}
		}
	}
	
	void updateTablesCache() {
		logger.info("Update tables");
		Set<String> cached;
		try {
			cached = Files.list(liblouisTablesPath)
						.map(v->v.getName(v.getNameCount()-1).toString())
						.collect(Collectors.toCollection(HashSet::new));
		} catch (IOException e) {
			if (logger.isLoggable(Level.FINE)) {
				logger.log(Level.FINE, "Failed to list existing tables. Cache may be contain removed files: " + liblouisTablesPath, e);
			}
			cached = new HashSet<>();
		}
		//Extract tables
		for (String s : tables) {
			try {
				cached.remove(s);
				Files.copy(getResourceAsStream("resource-files/tables/"+s), liblouisTablesPath.resolve(s), StandardCopyOption.REPLACE_EXISTING);
			} catch (IOException e) {
				// If this happens, the cache should be manually removed and the cache is automatically
				// re-created the next time the system starts.
				logger.log(Level.WARNING, "Failed to update existing table: " + liblouisTablesPath.resolve(s), e);
			}
		}
		for (String s : cached) {
			try {
				Files.delete(liblouisTablesPath.resolve(s));
			} catch (IOException e) {
				if (logger.isLoggable(Level.FINE)) {
					logger.log(Level.FINE, "Failed to delete a table: " + liblouisTablesPath.resolve(s), e);
				}
			}
		}
	}
	
	static LiblouisTables getInstance() {
		return INSTANCE;
	}
	
	List<String> getTableList() {
		return tables;
	}
	
	Path getTablePath(String table) {
		return liblouisTablesPath.resolve(table);
	}
	
	private List<String> getTables() {
		try {
			return Collections.unmodifiableList(getResourceFiles("resource-files/tables"));
		} catch (IOException e) {
			e.printStackTrace();
			return Collections.emptyList();
		}
	}

	private List<String> getResourceFiles(String path) throws IOException {
		List<String> filenames = new ArrayList<>();

		try (
				InputStream in = getResourceAsStream(path);
				BufferedReader br = new BufferedReader(new InputStreamReader(in))) {
			String resource;

			while ((resource = br.readLine()) != null) {
				filenames.add(resource);
			}
		}

		return filenames;
	}

	private InputStream getResourceAsStream(String resource) {
		final InputStream in = getContextClassLoader().getResourceAsStream(resource);
		return in == null ? getClass().getResourceAsStream(resource) : in;
	}

	private ClassLoader getContextClassLoader() {
		return Thread.currentThread().getContextClassLoader();
	}
}
