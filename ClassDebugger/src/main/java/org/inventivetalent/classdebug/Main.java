package org.inventivetalent.classdebug;

import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import org.inventivetalent.reflection.resolver.ClassResolver;
import org.inventivetalent.reflection.util.AccessUtil;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.logging.*;

public class Main implements Runnable {

	static final Logger log = Logger.getLogger("ClassDebug");

	public String      libFile;
	public DebugTarget target;
	public String      className;

	public static void main(String... args) {
		OptionParser optionParser = new OptionParser();
		optionParser.allowsUnrecognizedOptions();
		OptionSpec<String> fileOptionSpec = optionParser.accepts("file").withRequiredArg().ofType(String.class).defaultsTo("");
		OptionSpec<DebugTarget> debugTargetOptionSpec = optionParser.accepts("target").withRequiredArg().ofType(DebugTarget.class).defaultsTo(DebugTarget.FIELDS);
		OptionSpec<String> classStringOptionSpec = optionParser.accepts("class").withRequiredArg().ofType(String.class).defaultsTo("java.lang.String");

		OptionSet optionSet = optionParser.parse(args);

		Main main = new Main();
		main.libFile = fileOptionSpec.value(optionSet);
		main.target = debugTargetOptionSpec.value(optionSet);
		main.className = classStringOptionSpec.value(optionSet);

		log.setLevel(Level.ALL);
		log.setUseParentHandlers(false);
		ConsoleHandler handler = new ConsoleHandler();
		handler.setFormatter(new Formatter() {
			@Override
			public String format(LogRecord record) {
				return record.getMessage() + "\r\n";
			}
		});
		log.addHandler(handler);

		new Thread(main).start();
	}

	@Override
	public void run() {
		ClassResolver classResolver = new ClassResolver();
		Class<?> clazz;
		if (libFile != null && !libFile.isEmpty()) {
			try {
				URLClassLoader classLoader = new URLClassLoader(new URL[] { new File(libFile).toURI().toURL() }, getClass().getClassLoader());
				clazz = Class.forName(className, true, classLoader);
			} catch (MalformedURLException e) {
				log.log(Level.SEVERE, "Invalid file path: " + libFile, e);
				return;
			} catch (ClassNotFoundException e) {
				log.log(Level.SEVERE, "Class '" + className + "' not found", e);
				return;
			}
		} else {
			try {
				clazz = classResolver.resolve(className);
			} catch (ClassNotFoundException e) {
				log.log(Level.SEVERE, "Class '" + className + "' not found", e);
				return;
			}
		}

		log.info("--- Debugging " + target + " in class '" + className + "' ---");

		switch (target) {
			case FIELDS: {
				log.info("-- Fields");
				for (Field field : clazz.getDeclaredFields()) {
					try {
						field = AccessUtil.setAccessible(field);
						log.info(String.format("%-30.30s %-40.100s %s", Modifier.toString(field.getModifiers()), field.getType(), field.getName()));
					} catch (Exception e) {
						log.log(Level.SEVERE, "", e);
					}
				}
			}
			break;
			case METHODS: {
				log.info("-- Methods");
				for (Method method : clazz.getDeclaredMethods()) {
					try {
						method = AccessUtil.setAccessible(method);
						log.info("Name: " + method.getName());
						log.info("- Parameters");
						for (Class cl : method.getParameterTypes()) {
							log.info("  " + cl);
						}
					} catch (Exception e) {
						log.log(Level.SEVERE, "", e);
					}
				}
			}
			break;
			case CONSTRUCTORS: {
				log.info("-- Constructors");
				for (Constructor constructor : clazz.getDeclaredConstructors()) {
					try {
						constructor = AccessUtil.setAccessible(constructor);
						log.info("Name: " + constructor.getName());
						log.info("- Parameters");
						for (Class cl : constructor.getParameterTypes()) {
							log.info("  " + cl);
						}
					} catch (Exception e) {
						log.log(Level.SEVERE, "", e);
					}
				}
			}
			break;
		}
	}

	public enum DebugTarget {
		FIELDS,
		METHODS,
		CONSTRUCTORS
	}

}
