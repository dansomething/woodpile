/*
 * Project: com.hudren.woodpile
 * File:    LogEvent.java
 *
 * Author:  Jeff Hudren
 * Created: May 14, 2006
 *
 * Copyright (c) 2006-2013 Hudren Andromeda Connection. All rights reserved. 
 * 
 * The use and distribution terms for this software are covered by the
 * Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
 * which can be found in the file epl-v10.html at the root of this distribution.
 * 
 * By using this software in any fashion, you are agreeing to be bound by
 * the terms of this license.
 * 
 * You must not remove this notice, or any other, from this software.
 */

package com.hudren.woodpile.model;

import java.io.Serializable;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

import org.apache.log4j.Level;
import org.apache.log4j.spi.LoggingEvent;

/**
 * TODO LogEvent description
 * 
 * @author Jeff Hudren
 */
public class LogEvent
	implements Serializable
{

	private static final String NL = System.getProperty( "line.separator" );

	private static final DateFormat df = new SimpleDateFormat( "yyyy-MM-dd HH:mm:ss.SSS z" );

	private static final ConcurrentHashMap<String, String> HOSTS = new ConcurrentHashMap<String, String>();

	/**
	 * Serial version UID.
	 */
	private static final long serialVersionUID = 3759016573473947910L;

	private final long timeStamp;

	private final String loggerName;

	private final Level level;

	private final String renderedMessage;

	private final String threadName;

	private final String host;

	private final String server;

	private final String[] throwableStrRep;

	public LogEvent( final String host, final LoggingEvent event )
	{
		this.timeStamp = event.timeStamp;
		this.loggerName = event.getLoggerName();
		this.level = event.getLevel();
		this.renderedMessage = event.getRenderedMessage();
		this.threadName = event.getThreadName();
		this.host = lookupHost( host );
		this.server = getComponent( event );
		this.throwableStrRep = event.getThrowableStrRep();
	}

	public LogEvent( final String host, final org.apache.logging.log4j.core.LogEvent event )
	{
		Throwable thrown = event.getThrown();
		String[] rep = null;
		if ( thrown != null )
		{
			StackTraceElement[] stack = thrown.getStackTrace();
			rep = new String[ stack.length + 1 ];

			int i = 1;
			rep[ 0 ] = thrown.getClass().getName() + ": " + thrown.getLocalizedMessage();
			for ( StackTraceElement element : stack )
				rep[ i++ ] = "    at " + element.toString();
		}

		this.timeStamp = event.getMillis();
		this.loggerName = event.getLoggerName();
		this.level = Level.toLevel( event.getLevel().name() );
		this.renderedMessage = event.getMessage().getFormattedMessage();
		this.threadName = event.getThreadName();
		this.host = lookupHost( host );
		this.server = getComponent( event.getContextMap() );
		this.throwableStrRep = rep;
	}

	public LogEvent( final String host, final Map<String, String> fields )
	{
		this.timeStamp = Long.valueOf( fields.get( "timestamp" ) );
		this.loggerName = fields.get( "logger" );
		this.level = Level.toLevel( fields.get( "level" ).toUpperCase() );
		this.renderedMessage = fields.get( "message" );
		this.threadName = fields.get( "thread" );
		this.host = lookupHost( host );
		this.server = getComponent( fields );

		String throwable = fields.get( "throwable" );
		this.throwableStrRep = throwable != null ? throwable.split( "\n" ) : null;
	}

	private static String lookupHost( final String host )
	{
		String name = HOSTS.get( host );
		if ( name == null )
		{
			name = host;

			Pattern ipPattern = Pattern.compile( "\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}" );
			if ( ipPattern.matcher( host ).matches() )
			{
				try
				{
					name = InetAddress.getByName( host ).getHostName();
				}
				catch ( UnknownHostException e )
				{
				}
			}

			HOSTS.putIfAbsent( host, name );
		}

		return name;
	}

	private static String getComponent( LoggingEvent event )
	{
		Object component = event.getMDC( "component" );
		if ( component == null )
			component = event.getMDC( "server" );
		if ( component == null )
			component = event.getMDC( "application" );

		return component != null ? component.toString() : null;
	}

	private static String getComponent( Map<String, String> context )
	{
		String component = context.get( "component" );
		if ( component == null )
			component = context.get( "server" );
		if ( component == null )
			component = context.get( "application" );

		return component != null ? component : null;
	}

	/**
	 * Getter for host
	 * 
	 * @return host
	 */
	public String getHost()
	{
		return host;
	}

	/**
	 * Getter for server
	 * 
	 * @return server
	 */
	public String getServer()
	{
		return server;
	}

	/**
	 * Getter for level
	 * 
	 * @return level
	 */
	public Level getLevel()
	{
		return level;
	}

	/**
	 * Getter for levelName
	 * 
	 * @return level
	 */
	public String getLevelName()
	{
		return level.toString();
	}

	/**
	 * Getter for loggerName
	 * 
	 * @return loggerName
	 */
	public String getLoggerName()
	{
		return loggerName;
	}

	/**
	 * Getter for renderedMessage
	 * 
	 * @return renderedMessage
	 */
	public String getRenderedMessage()
	{
		return renderedMessage;
	}

	/**
	 * Getter for threadName
	 * 
	 * @return threadName
	 */
	public String getThreadName()
	{
		return threadName;
	}

	/**
	 * Getter for throwableStrRep
	 * 
	 * @return throwableStrRep
	 */
	public String[] getThrowableStrRep()
	{
		return throwableStrRep;
	}

	/**
	 * Getter for timeStamp
	 * 
	 * @return timeStamp
	 */
	public long getTimeStamp()
	{
		return timeStamp;
	}

	public String getStrRep()
	{
		final StringBuilder buffer = new StringBuilder();

		buffer.append( "<b>Level:</b>      " ).append( level ).append( NL );
		buffer.append( "<b>Time:</b>       " ).append( df.format( timeStamp ) ).append( NL );
		buffer.append( "<b>Logger:</b>     " ).append( loggerName ).append( NL );

		if ( server != null )
		{
			buffer.append( "<b>Component:</b>  " ).append( server );

			if ( host != null && !"localhost".equals( host ) )
				buffer.append( " on " ).append( host );

			buffer.append( NL );
		}
		else if ( host != null && !"localhost".equals( host ) )
			buffer.append( "<b>Host:</b>       " ).append( host ).append( NL );

		buffer.append( "<b>Thread:</b>     " ).append( threadName ).append( NL );
		buffer.append( "<b>Message:</b>    " ).append( renderedMessage );

		if ( throwableStrRep != null && throwableStrRep.length > 0 )
		{
			buffer.append( NL ).append( NL );
			buffer.append( "<red>" );

			for ( final String line : throwableStrRep )
			{
				buffer.append( line ).append( NL );
			}

			buffer.append( "</red>" ).append( NL );
		}

		return buffer.toString();
	}

}
