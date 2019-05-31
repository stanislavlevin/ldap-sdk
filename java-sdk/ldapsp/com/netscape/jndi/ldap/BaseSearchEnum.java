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

import javax.naming.Name;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;

import com.netscape.jndi.ldap.common.ExceptionMapper;

import netscape.ldap.LDAPEntry;
import netscape.ldap.LDAPException;
import netscape.ldap.LDAPReferralException;
import netscape.ldap.LDAPSearchResults;

/**
 * Wrapper for the LDAPSearchResult that implements all NamingEnumeration methods
 * except next() (left to be implemented by subclasses). Because LDAPJDK does
 * not provide for capability to ignoral referrals, the class is using hasMore()
 * method to read ahead search results and "ignore" referrals if required.
 * Base class for BindingEnum, NameClassPairEnum and SearchResultEnum
 */
abstract class BaseSearchEnum<T> implements NamingEnumeration<T> {

    LDAPSearchResults m_res;
    LdapContextImpl m_ctx;
    Name m_ctxName;
    private LDAPEntry nextEntry;
    private LDAPException nextException;

    public BaseSearchEnum(LDAPSearchResults res, LdapContextImpl ctx) throws NamingException{
        m_res = res;
        m_ctx = ctx;
        try {
            m_ctxName = LdapNameParser.getParser().parse(m_ctx.m_ctxDN);
        }
        catch ( NamingException e ) {
            throw ExceptionMapper.getNamingException(e);
        }
    }

    LDAPEntry nextLDAPEntry() throws NamingException{
        if (nextException == null && nextEntry == null) {
            hasMore();
        }
        try {
            if (nextException != null) {
                if (nextException instanceof LDAPReferralException) {
                    throw new LdapReferralException(m_ctx,
                              (LDAPReferralException)nextException);
                }
                else {
                    throw ExceptionMapper.getNamingException(nextException);
                }
            }
            return nextEntry;
        }
        finally {
            nextException = null;
            nextEntry = null;
        }
    }

    public T nextElement() {
        try {
            return next();
        }
        catch ( Exception e ) {
            System.err.println( "Error in BaseSearchEnum.nextElement(): " + e.toString() );
            e.printStackTrace(System.err);
            return null;
        }
    }

    public boolean hasMore() throws NamingException{

        if (nextEntry != null || nextException != null) {
            return true;
        }

        if (m_res.hasMoreElements()) {
            try {
                nextEntry = m_res.next();
                return true;
            }
            catch (LDAPReferralException e) {
                boolean ignoreReferrals = m_ctx.m_ctxEnv.ignoreReferralsMode();
                if (ignoreReferrals) {

                    return hasMore();

                    // PARTIAL_SEARCH_RESULT should be thrown according to the
                    // Implmentation Guidelines for LDAPSP, but is not done by
                    // the Sun LDAPSP 1.2, so we not doing it either.
                    //nextException = new LDAPException("Ignoring referral", 9);
                    //return true;
                }
                else {
                    nextException = e;
                    return true;
                }
            }
            catch ( LDAPException e ) {
                nextException = e;
                return true;
            }
        }
        return false;
    }

    public boolean hasMoreElements() {
        try {
            return hasMore();
        }
        catch ( Exception e ) {
            System.err.println( "Error in BaseSearchEnum.hasMoreElements(): " + e.toString() );
            e.printStackTrace(System.err);
            return false;
        }

    }

    public void close() throws NamingException{
        try {
            m_ctx.m_ldapSvc.getConnection().abandon(m_res);
        }
        catch (LDAPException e) {
            throw ExceptionMapper.getNamingException(e);
        }
    }
}

