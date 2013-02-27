package com.comphenix.protocol.injector.server;

import java.io.FilterInputStream;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.Socket;
import java.net.SocketAddress;
import org.bukkit.Server;
import org.bukkit.entity.Player;

import com.comphenix.protocol.error.ErrorReporter;
import com.comphenix.protocol.reflect.FieldAccessException;
import com.comphenix.protocol.reflect.FieldUtils;
import com.comphenix.protocol.reflect.FuzzyReflection;

public abstract class AbstractInputStreamLookup {
	// Used to access the inner input stream of a filtered input stream
	private static Field filteredInputField;

	// Error reporter
	protected final ErrorReporter reporter;
	
	// Reference to the server itself
	protected final Server server;

	protected AbstractInputStreamLookup(ErrorReporter reporter, Server server) {
		this.reporter = reporter;
		this.server = server;
	}

	/**
	 * Retrieve the underlying input stream that is associated with a given filter input stream.
	 * @param filtered - the filter input stream.
	 * @return The underlying input stream that is being filtered.
	 * @throws FieldAccessException Unable to access input stream.
	 */
	protected static InputStream getInputStream(FilterInputStream filtered) {
		if (filteredInputField == null)
			filteredInputField = FuzzyReflection.fromClass(FilterInputStream.class, true).
								  getFieldByType("in", InputStream.class);
		
		InputStream current = filtered;
		
		try {
			// Iterate until we find the real input stream
			while (current instanceof FilterInputStream) {
				current = (InputStream) FieldUtils.readField(filteredInputField, current, true);
			}
			return current;
		} catch (IllegalAccessException e) {
			throw new FieldAccessException("Cannot access filtered input field.", e);
		}
	}

	/**
	 * Inject the given server thread or dedicated connection.
	 * @param container - class that contains a ServerSocket field.
	 */
	public abstract void inject(Object container);
	
	/**
	 * Invoked when the world has loaded.
	 */
	public abstract void postWorldLoaded();
	
	/**
	 * Retrieve the associated socket injector for a player.
	 * @param filtered - the indentifying filtered input stream.
	 * @return The socket injector we have associated with this player.
	 * @throws FieldAccessException Unable to access input stream.
	 */
	public SocketInjector getSocketInjector(FilterInputStream filtered) {
		return getSocketInjector(getInputStream(filtered));
	}

	/**
	 * Retrieve the associated socket injector for a player.
	 * @param input - the indentifying filtered input stream.
	 * @return The socket injector we have associated with this player.
	 */
	public abstract SocketInjector getSocketInjector(InputStream input);
	
	/**
	 * Retrieve an injector by its socket.
	 * @param socket - the socket.
	 * @return The socket injector.
	 */
	public abstract SocketInjector getSocketInjector(Socket socket);
	
	/**
	 * Retrieve a injector by its address.
	 * @param address - the address of the socket.
	 * @return The socket injector, or NULL if not found.
	 */
	public abstract SocketInjector getSocketInjector(SocketAddress address);

	/**
	 * Associate a given socket the provided socket injector.
	 * @param input - the socket to associate.
	 * @param injector - the injector.
	 */
	public abstract void setSocketInjector(Socket socket, SocketInjector injector);

	/**
	 * If a player can hold a reference to its parent injector, this method will update that reference.
	 * @param previous - the previous injector.
	 * @param current - the new injector.
	 */
	protected void onPreviousSocketOverwritten(SocketInjector previous, SocketInjector current) {
		Player player = previous.getPlayer();
		
		// Default implementation
		if (player instanceof InjectContainer) {
			InjectContainer container = (InjectContainer) player;
			container.setInjector(current);
		}
	}
	
	/**
	 * Invoked when the injection should be undone.
	 */
	public abstract void cleanupAll();
}