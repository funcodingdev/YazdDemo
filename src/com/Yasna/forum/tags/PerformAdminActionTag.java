/**
 * Copyright (C) 2001 Yasna.com. All rights reserved.
 *
 * ===================================================================
 * The Apache Software License, Version 1.1
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution,
 *    if any, must include the following acknowledgment:
 *       "This product includes software developed by
 *        Yasna.com (http://www.yasna.com)."
 *    Alternately, this acknowledgment may appear in the software itself,
 *    if and wherever such third-party acknowledgments normally appear.
 *
 * 4. The names "Yazd" and "Yasna.com" must not be used to
 *    endorse or promote products derived from this software without
 *    prior written permission. For written permission, please
 *    contact yazd@yasna.com.
 *
 * 5. Products derived from this software may not be called "Yazd",
 *    nor may "Yazd" appear in their name, without prior written
 *    permission of Yasna.com.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL YASNA.COM OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of Yasna.com. For more information
 * on Yasna.com, please see <http://www.yasna.com>.
 */

package com.Yasna.forum.tags;

import javax.servlet.jsp.tagext.*;
import com.Yasna.forum.*;
import javax.servlet.jsp.*;
import java.net.*;
import java.util.*;

/**
 * Jsp tag evaluates its body if curent logged in user is admin or moderator.
 * Must be nested in Thread or Forum tag. Adds 'delete' and 'approve' functionality
 * to current parent container. Type of parent container must be specified in
 * the [id] parameter. Two values are available - "thread" and "forum".
 */
public class PerformAdminActionTag extends TagSupport {

    private Action target;

    public int doStartTag() throws JspException {
        try {
			Authorization authToken = getAuthToken();
            ForumFactory forumFactory = ForumFactory.getInstance(authToken);
            ForumPermissions permissions = forumFactory.getPermissions(authToken);
            boolean isSystemAdmin = permissions.get(ForumPermissions.SYSTEM_ADMIN);
            boolean isUserAdmin   = permissions.get(ForumPermissions.FORUM_ADMIN);
            boolean isModerator   = false;
            Forum forum = getCurrentForum();
            if (forum != null) {
                isModerator   = forum.getPermissions(authToken).get(ForumPermissions.MODERATOR);
            }
            boolean isAdmin = isUserAdmin || isSystemAdmin || isModerator;
            if (isAdmin) {
                Enumeration en = pageContext.getRequest().getParameterNames();
                while (en.hasMoreElements()) {
                    String p = (String) en.nextElement();
                    if (p.startsWith("approve_")) {
                        try {
                            String idStr = p.substring(8, p.indexOf("."));
                            int id = Integer.parseInt(idStr);
                            target.setApprovement(id, true);
                            break;
                        } catch(Exception e) {
                            e.printStackTrace();
                        }
                    } else if (p.startsWith("delete_")) {
                        try {
                            String idStr = p.substring(7, p.indexOf("."));
                            int id = Integer.parseInt(idStr);
                            target.delete(id);
                            break;
                        } catch(Exception e) {
                            e.printStackTrace();
                        }
                    }
                }

            }//if isAdmin
        } catch(Exception e) {
        }
        return SKIP_BODY;
    }

    /**
     * Sets the parent type of parent container.
     * @param t String containing one of both values "thread" and "forum".
     * @throws Exception
     */
    public void setTarget(String t) throws Exception {
        if ("thread".equals(t)) {
            target = new TargetThread();
        } else if ("forum".equals(t)) {
            target = new TargetForum();
        } else {
            throw new Exception("Unknown 'target' = [" + target + "] set for attribute on PerformAdminAction tag!");
        }
    }

    /**
     * Retrieve Authorization object from pageContext which give the information
     * for currently logged user.
     *
     * @return Authorization object.
     */
    private Authorization getAuthToken() {
        YazdState js = (YazdState) pageContext.getAttribute("yazdUserState",PageContext.SESSION_SCOPE);
		return js.getAuthorization();
    }

    private ForumThread getCurrentThread() {
        ThreadTag tt = null;
        try {
            tt = (ThreadTag)this.findAncestorWithClass(this,
                Class.forName("com.Yasna.forum.tags.ThreadTag"));
        } catch(Exception e) {
        }
        return tt.getThread();
    }

    private Forum getCurrentForum() {
        ForumTag tt = null;
        try {
            tt = (ForumTag)this.findAncestorWithClass(this,
                Class.forName("com.Yasna.forum.tags.ForumTag"));
        } catch(Exception e) {
        }
        return tt.getForum();
    }

    interface Action {
        public void delete(int id) throws Exception;
        public void setApprovement(int id, boolean approvement) throws Exception;
    }
    class TargetThread implements Action {
        public void delete(int id) throws Exception {
            ForumThread thread = getCurrentThread();
            ForumMessage toDelete = thread.getMessage(id);
            thread.deleteMessage(toDelete);
        }
        public void setApprovement(int id, boolean approvement) throws Exception {
            ForumThread thread = getCurrentThread();
            thread.getMessage(id).setApprovment(approvement);
        }
    }
    class TargetForum implements Action {
        public void delete(int id) throws Exception {
            Forum forum = getCurrentForum();
            ForumThread toDelete = forum.getThread(id);
            forum.deleteThread(toDelete);
        }
        public void setApprovement(int id, boolean approvement) throws Exception {
            Forum forum = getCurrentForum();
            forum.getThread(id).setApprovment(approvement);
        }
    }

}