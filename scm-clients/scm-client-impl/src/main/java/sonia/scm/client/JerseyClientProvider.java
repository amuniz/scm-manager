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

import org.sonatype.spice.jersey.client.ahc.config.DefaultAhcConfig;

import sonia.scm.ScmState;
import sonia.scm.util.AssertUtil;
import sonia.scm.util.Util;

//~--- JDK imports ------------------------------------------------------------

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.core.util.MultivaluedMapImpl;
import com.sun.servicetag.UnauthorizedAccessException;

import javax.ws.rs.core.MultivaluedMap;

/**
 *
 * @author Sebastian Sdorra
 */
public class JerseyClientProvider implements ScmClientProvider
{

  /**
   * Method description
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
  @Override
  public ScmClientSession createSession(String url, String username,
          String password)
          throws ScmClientException
  {
    AssertUtil.assertIsNotEmpty(url);

    ScmUrlProvider urlProvider = new ScmUrlProvider(url);
    DefaultAhcConfig config = new DefaultAhcConfig();
    Client client = Client.create(config);
    WebResource resource = client.resource(urlProvider.getAuthenticationUrl());
    ClientResponse response = null;

    if (Util.isNotEmpty(username) && Util.isNotEmpty(password))
    {
      MultivaluedMap<String, String> formData = new MultivaluedMapImpl();

      formData.add("username", username);
      formData.add("password", password);
      response = resource.type("application/x-www-form-urlencoded").post(
        ClientResponse.class, formData);
    }
    else
    {
      response = resource.get(ClientResponse.class);
    }

    if (response.getStatus() == 401)
    {
      throw new UnauthorizedAccessException();
    }

    ScmState state = response.getEntity(ScmState.class);

    if (!state.isSuccess())
    {
      throw new ScmClientException("create ScmClientSession failed");
    }

    return new JerseyClientSession(client, urlProvider, state);
  }
}