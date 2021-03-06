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



package sonia.scm.repository.spi;

//~--- non-JDK imports --------------------------------------------------------

import com.aragost.javahg.Repository;
import com.aragost.javahg.RepositoryConfiguration;

import com.google.common.base.Strings;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sonia.scm.repository.HgConfig;
import sonia.scm.repository.spi.javahg.HgFileviewExtension;

//~--- JDK imports ------------------------------------------------------------

import java.io.Closeable;
import java.io.File;
import java.io.IOException;

import java.nio.charset.Charset;

/**
 *
 * @author Sebastian Sdorra
 */
public class HgCommandContext implements Closeable
{

  /** Field description */
  private static final String PROPERTY_ENCODING = "hg.encoding";

  /**
   * the logger for HgCommandContext
   */
  private static final Logger logger =
    LoggerFactory.getLogger(HgCommandContext.class);

  //~--- constructors ---------------------------------------------------------

  /**
   * Constructs ...
   *
   *
   * @param config
   * @param repository
   * @param directory
   */
  public HgCommandContext(HgConfig config,
    sonia.scm.repository.Repository repository, File directory)
  {
    this(config, repository, directory, false);
  }

  /**
   * Constructs ...
   *
   *
   * @param config
   * @param repository
   * @param directory
   * @param pending
   */
  public HgCommandContext(HgConfig config,
    sonia.scm.repository.Repository repository, File directory, boolean pending)
  {
    this.config = config;
    this.directory = directory;
    this.encoding = repository.getProperty(PROPERTY_ENCODING);
    this.pending = pending;

    if (Strings.isNullOrEmpty(encoding))
    {
      encoding = config.getEncoding();
    }
  }

  //~--- methods --------------------------------------------------------------

  /**
   * Method description
   *
   *
   * @throws IOException
   */
  @Override
  public void close() throws IOException
  {
    if (repository != null)
    {
      repository.close();
    }
  }

  /**
   * Method description
   *
   *
   * @return
   */
  public Repository open()
  {
    if (repository == null)
    {
      RepositoryConfiguration repoConfiguration =
        RepositoryConfiguration.DEFAULT;

      repoConfiguration.addExtension(HgFileviewExtension.class);
      repoConfiguration.setEnablePendingChangesets(pending);

      try
      {
        Charset charset = Charset.forName(encoding);

        if (logger.isTraceEnabled())
        {
          logger.trace("set encoding {} for mercurial", encoding);
        }

        repoConfiguration.setEncoding(charset);
      }
      catch (IllegalArgumentException ex)
      {
        logger.error("could not set encoding for mercurial", ex);
      }

      repoConfiguration.setHgBin(config.getHgBinary());
      repository = Repository.open(repoConfiguration, directory);
    }

    return repository;
  }

  //~--- get methods ----------------------------------------------------------

  /**
   * Method description
   *
   *
   * @return
   */
  public HgConfig getConfig()
  {
    return config;
  }

  //~--- fields ---------------------------------------------------------------

  /** Field description */
  private HgConfig config;

  /** Field description */
  private File directory;

  /** Field description */
  private String encoding;

  /** Field description */
  private boolean pending;

  /** Field description */
  private Repository repository;
}
