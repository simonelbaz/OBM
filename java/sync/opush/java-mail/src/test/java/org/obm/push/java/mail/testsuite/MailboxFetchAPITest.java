package org.obm.push.java.mail.testsuite;

import org.obm.push.java.mail.MailEnvModule;
import org.obm.push.mail.imap.GuiceModule;

@GuiceModule(MailEnvModule.class)
public class MailboxFetchAPITest extends org.obm.push.mail.imap.testsuite.MailboxFetchAPITest {
}
