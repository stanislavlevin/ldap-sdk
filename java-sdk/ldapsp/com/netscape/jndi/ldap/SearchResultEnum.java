/* -*- Mode: C++; tab-width: 4; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Mozilla Public License Version
 * 1.1 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 * for the specific language governing rights and limitations under the
 * License.
 *
 * The Original Code is mozilla.org code.
 *
 * The Initial Developer of the Original Code is
 * Netscape Communications Corporation.
 * Portions created by the Initial Developer are Copyright (C) 1999
 * the Initial Developer. All Rights Reserved.
 *
 * Contributor(s):
 *
 * Alternatively, the contents of this file may be used under the terms of
 * either the GNU General Public License Version 2 or later (the "GPL"), or
 * the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the MPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the MPL, the GPL or the LGPL.
 *
 * ***** END LICENSE BLOCK ***** */
package com.netscape.jndi.ldap;

import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.Control;

import com.netscape.jndi.ldap.controls.NetscapeControlFactory;

import netscape.ldap.LDAPControl;
import netscape.ldap.LDAPEntry;
import netscape.ldap.LDAPSearchResults;

/**
 * A wrapper for the LDAPSeatchResults. Convert a LDAPSearchResults enumeration
 * of LDAPEntries into a JNDI NamingEnumeration of JNDI SearchResults.
 */
class SearchResultEnum extends BaseSearchEnum<SearchResult> {

    boolean m_returnObjs; // ReturningObjFlag in SearchControls
    String[] m_userBinaryAttrs;

    public SearchResultEnum(LDAPSearchResults res, boolean returnObjs, LdapContextImpl ctx) throws NamingException{
        super(res, ctx);
        m_returnObjs = returnObjs;
        m_userBinaryAttrs = ctx.m_ctxEnv.getUserDefBinaryAttrs();
    }

    public SearchResult next() throws NamingException{
        LDAPEntry entry = nextLDAPEntry();
        String name = LdapNameParser.getRelativeName(m_ctxName, entry.getDN());
        Object obj = (m_returnObjs) ? ObjectMapper.entryToObject(entry, m_ctx) : null;
        Attributes attrs = new AttributesImpl(entry.getAttributeSet(), m_userBinaryAttrs);

        // check for response controls
        LDAPControl[] ldapCtls = m_res.getResponseControls();
        if (ldapCtls != null) {
            // Parse raw controls
            Control[] ctls = new Control[ldapCtls.length];
            for (int i=0; i < ldapCtls.length; i++) {
                ctls[i] = NetscapeControlFactory.getControlInstance(ldapCtls[i]);
                if (ctls[i] == null) {
                    throw new NamingException("Unsupported control " + ldapCtls[i].getID());
                }
            }

            SearchResultWithControls searchRes =
                new SearchResultWithControls(name, obj, attrs);
            searchRes.setControls(ctls);

            return searchRes;
        }
        else { // no controls
            return new SearchResult(name, obj, attrs);
        }
    }
}
