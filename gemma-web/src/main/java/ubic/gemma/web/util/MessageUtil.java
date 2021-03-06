package ubic.gemma.web.util;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.Locale;

/*
 * The Gemma project
 * 
 * Copyright (c) 2012 University of British Columbia
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

/**
 * Provides methods for putting messages to the user in the session.
 *
 * @author paul
 */
public interface MessageUtil {

    /**
     * Convenience method for getting a i18n key's value.
     * Implementation note: Calling getMessageSourceAccessor() is used because the RequestContext variable is not set in
     * unit tests b/c there's no DispatchServlet Request.
     *
     * @param msgKey key
     * @param locale the current locale
     * @return text
     */
    String getText( String msgKey, Locale locale );

    /**
     * Convenience method for getting a i18n key's value with arguments.
     *
     * @param msgKey key
     * @param args   args
     * @param locale the current locale
     * @return text
     */
    String getText( String msgKey, Object[] args, Locale locale );

    /**
     * Convenient method for getting a i18n key's value with a single string argument.
     *
     * @param msgKey message key
     * @param arg    argument
     * @param locale the current locale
     * @return text
     */
    String getText( String msgKey, String arg, Locale locale );

    /**
     * Put a message into the session. These can be displayed to the user.
     * Messages accumulate in a list until they are viewed in messages.jsp - at which point they are removed from the
     * session.
     *
     * @param request request
     * @param msg     msg
     */
    void saveMessage( HttpServletRequest request, String msg );

    /**
     * Put a message into the session. These can be displayed to the user.
     * Messages accumulate in a list until they are viewed in messages.jsp - at which point they are removed from the
     * session.
     *
     * @param request        request
     * @param parameter      parameter to be filled into the message.
     * @param defaultMessage default message
     */
    void saveMessage( HttpServletRequest request, String key, Object parameter, String defaultMessage );

    /**
     * Put a message into the session. These can be displayed to the user.
     * Messages accumulate in a list until they are viewed in messages.jsp - at which point they are removed from the
     * session.
     *
     * @param request        request
     * @param parameters     Array of parameters to be filled into the message.
     * @param defaultMessage default message
     */
    void saveMessage( HttpServletRequest request, String key, Object[] parameters, String defaultMessage );

    /**
     * Put a message into the session. These can be displayed to the user.
     * Messages accumulate in a list until they are viewed in messages.jsp - at which point they are removed from the
     * session.
     *
     * @param request        request
     * @param key            key
     * @param defaultMessage default message
     */
    void saveMessage( HttpServletRequest request, String key, String defaultMessage );

    /**
     * Put a message into the session. These can be displayed to the user.
     * Messages accumulate in a list until they are viewed in messages.jsp - at which point they are removed from the
     * session.
     *
     * @param session session
     * @param msg     msg
     */
    void saveMessage( HttpSession session, String msg );

}