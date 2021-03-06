/**
 * Copyright (c) 2010, Sebastian Sdorra
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 3. Neither the name of SCM-Manager; nor the names of its
 *    contributors may be used to endorse or promote products derived from this
 *    software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE REGENTS OR CONTRIBUTORS BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * http://bitbucket.org/sdorra/scm-manager
 *
 */



package sonia.scm.client;

//~--- non-JDK imports --------------------------------------------------------

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sonia.scm.util.ServiceUtil;

/**
 *
 * @author Sebastian Sdorra
 */
public class ScmClient
{

  /** Field description */
  private static volatile ScmClientProvider provider = null;

  /** the logger for ScmClient */
  private static final Logger logger = LoggerFactory.getLogger(ScmClient.class);

  //~--- methods --------------------------------------------------------------

  /**
   * Creates an ScmClientSession for the given user
   *
   *
   * @param url
   * @param username
   * @param password
   *
   * @return
   *
   * @throws ScmClientException
   */
  public static ScmClientSession createSession(String url, String username,
          String password)
          throws ScmClientException
  {
    return getProvider().createSession(url, username, password);
  }

  /**
   * Creates an anonymous ScmClientSession
   *
   *
   * @param url
   *
   * @return
   *
   * @throws ScmClientException
   */
  public static ScmClientSession createSession(String url)
          throws ScmClientException
  {
    return getProvider().createSession(url, null, null);
  }

  //~--- get methods ----------------------------------------------------------

  /**
   * Method description
   *
   *
   * @return
   *
   */
  private static ScmClientProvider getProvider()
  {
    if (provider == null)
    {
      synchronized (ScmClientProvider.class)
      {
        if (provider == null)
        {
          provider = ServiceUtil.getService(ScmClientProvider.class);
        }
      }
    }

    if (provider == null)
    {
      throw new ScmClientException("could not find a ScmClientProvider");
    }
    else if (logger.isInfoEnabled())
    {
      logger.info("create ScmClient with provider {}",
                  provider.getClass().getName());
    }

    return provider;
  }
}
