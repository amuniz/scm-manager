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



package sonia.scm.web;

//~--- non-JDK imports --------------------------------------------------------

import com.google.common.collect.Lists;

import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.transport.PostReceiveHook;
import org.eclipse.jgit.transport.PreReceiveHook;
import org.eclipse.jgit.transport.ReceiveCommand;
import org.eclipse.jgit.transport.ReceivePack;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sonia.scm.io.Command;
import sonia.scm.io.CommandResult;
import sonia.scm.io.SimpleCommand;
import sonia.scm.repository.GitRepositoryHandler;
import sonia.scm.repository.GitRepositoryHookEvent;
import sonia.scm.repository.GitUtil;
import sonia.scm.repository.RepositoryHookType;
import sonia.scm.repository.RepositoryManager;
import sonia.scm.repository.RepositoryUtil;
import sonia.scm.util.IOUtil;
import sonia.scm.util.Util;

//~--- JDK imports ------------------------------------------------------------

import java.io.File;
import java.io.IOException;

import java.util.Collection;
import java.util.List;

/**
 *
 * @author Sebastian Sdorra
 */
public class GitReceiveHook implements PreReceiveHook, PostReceiveHook
{

  /** Field description */
  public static final String FILE_HOOKDIRECTORY = "hooks";

  /** Field description */
  public static final String FILE_HOOK_POST_RECEIVE = "post-receive";

  /** Field description */
  public static final String FILE_HOOK_PRE_RECEIVE = "pre-receive";

  /** Field description */
  public static final String PREFIX_MSG = "[SCM] ";

  /** the logger for GitReceiveHook */
  private static final Logger logger =
    LoggerFactory.getLogger(GitReceiveHook.class);

  //~--- constructors ---------------------------------------------------------

  /**
   * Constructs ...
   *
   *
   * @param repositoryManager
   * @param handler
   */
  public GitReceiveHook(RepositoryManager repositoryManager,
    GitRepositoryHandler handler)
  {
    this.repositoryManager = repositoryManager;
    this.handler = handler;
  }

  //~--- methods --------------------------------------------------------------

  /**
   * Method description
   *
   *
   * @param rpack
   * @param receiveCommands
   */
  @Override
  public void onPostReceive(ReceivePack rpack,
    Collection<ReceiveCommand> receiveCommands)
  {
    onReceive(rpack, receiveCommands, RepositoryHookType.POST_RECEIVE);
  }

  /**
   * Method description
   *
   *
   *
   * @param rpack
   * @param receiveCommands
   */
  @Override
  public void onPreReceive(ReceivePack rpack,
    Collection<ReceiveCommand> receiveCommands)
  {
    onReceive(rpack, receiveCommands, RepositoryHookType.PRE_RECEIVE);
  }

  /**
   * Method description
   *
   * @param rpack
   * @param rc
   * @param repositoryDirectory
   * @param hook
   * @param oldId
   * @param newId
   * @param refName
   */
  private void executeFileHook(ReceivePack rpack, ReceiveCommand rc,
    File repositoryDirectory, File hook, ObjectId oldId, ObjectId newId,
    String refName)
  {
    if (logger.isDebugEnabled())
    {
      logger.debug("execute file hook '{}' in directoy '{}'");
    }

    final Command cmd = new SimpleCommand(hook.getAbsolutePath(),
                          GitUtil.getId(oldId), GitUtil.getId(newId),
                          Util.nonNull(refName));

    // issue-99
    cmd.setWorkDirectory(repositoryDirectory);

    try
    {
      CommandResult result = cmd.execute();

      if (result.isSuccessfull())
      {
        if (logger.isDebugEnabled())
        {
          logger.debug("executed file hook successfull");

          if (logger.isTraceEnabled())
          {
            String out = result.getOutput();

            if (Util.isNotEmpty(out))
            {
              logger.trace(out);
            }
          }
        }
      }
      else
      {
        if (logger.isErrorEnabled())
        {
          logger.error("failed to execute file hook");
        }

        String out = result.getOutput();

        if (Util.isNotEmpty(out))
        {
          logger.error(out);
        }

        sendError(rpack, rc, out);
      }
    }
    catch (IOException ex)
    {
      logger.error("could not execute file hook", ex);
    }
  }

  /**
   * Method description
   *
   *
   * @param rpack
   * @param rc
   * @param newId
   * @param type
   */
  private void handleFileHooks(ReceivePack rpack, ReceiveCommand rc,
    RepositoryHookType type)
  {
    ObjectId newId = rc.getNewId();
    ObjectId oldId = null;

    if (isUpdateCommand(rc))
    {
      oldId = rc.getOldId();

      if (logger.isTraceEnabled())
      {
        logger.trace("handle update receive command from commit '{}' to '{}'",
          oldId.getName(), newId.getName());
      }
    }
    else if (logger.isTraceEnabled())
    {
      logger.trace("handle receive command for commit '{}'", newId.getName());
    }

    File directory = rpack.getRepository().getDirectory();
    String scriptName = null;

    if (type == RepositoryHookType.POST_RECEIVE)
    {
      scriptName = FILE_HOOK_POST_RECEIVE;
    }
    else if (type == RepositoryHookType.PRE_RECEIVE)
    {
      scriptName = FILE_HOOK_PRE_RECEIVE;
    }

    if (scriptName != null)
    {
      File hookScript = getHookScript(directory, scriptName);

      if (hookScript != null)
      {
        executeFileHook(rpack, rc, directory, hookScript, oldId, newId,
          rc.getRefName());
      }
    }
  }

  /**
   * Method description
   *
   *
   * @param rpack
   * @param receiveCommands
   * @param type
   */
  private void handleReceiveCommands(ReceivePack rpack,
    List<ReceiveCommand> receiveCommands, RepositoryHookType type)
  {
    try
    {
      Repository repository = rpack.getRepository();
      String repositoryName = RepositoryUtil.getRepositoryName(handler,
                                repository.getDirectory());

      repositoryManager.fireHookEvent(GitRepositoryHandler.TYPE_NAME,
        repositoryName,
        new GitRepositoryHookEvent(rpack, receiveCommands, type));
    }
    catch (Exception ex)
    {
      logger.error("could not handle receive commands", ex);

      if (type == RepositoryHookType.PRE_RECEIVE)
      {
        sendError(rpack, receiveCommands, ex.getMessage());
      }
    }
  }

  /**
   * Method description
   *
   *
   * @param rpack
   * @param receiveCommands
   * @param type
   */
  private void onReceive(ReceivePack rpack,
    Collection<ReceiveCommand> receiveCommands, RepositoryHookType type)
  {
    if (logger.isTraceEnabled())
    {
      logger.trace("received git hook, type={}", type);
    }

    List<ReceiveCommand> commands = Lists.newArrayList();

    for (ReceiveCommand rc : receiveCommands)
    {
      if (isReceiveable(rc, type))
      {
        commands.add(rc);
        handleFileHooks(rpack, rc, type);
      }
      else if (logger.isTraceEnabled())
      {
        //J-
        logger.trace("skip receive command, type={}, ref={}, result={}",
          new Object[] { 
            rc.getType(),
            rc.getRefName(), 
            rc.getResult() 
          }
        );
        //J+
      }
    }

    if (!commands.isEmpty())
    {
      handleReceiveCommands(rpack, commands, type);
    }
    else if (logger.isDebugEnabled())
    {
      logger.debug("no receive command found to process");
    }
  }

  /**
   * Method description
   *
   *
   * @param rpack
   * @param commands
   * @param message
   */
  private void sendError(ReceivePack rpack, Iterable<ReceiveCommand> commands,
    String message)
  {
    if (logger.isWarnEnabled())
    {
      logger.warn("abort git push request with msg: {}", message);
    }

    for (ReceiveCommand rc : commands)
    {
      rc.setResult(ReceiveCommand.Result.REJECTED_OTHER_REASON);
    }

    rpack.sendError(PREFIX_MSG.concat(Util.nonNull(message)));
  }

  /**
   * Method description
   *
   *
   *
   * @param rpack
   * @param rc
   * @param message
   */
  private void sendError(ReceivePack rpack, ReceiveCommand rc, String message)
  {
    rc.setResult(ReceiveCommand.Result.REJECTED_OTHER_REASON);
    rpack.sendError(PREFIX_MSG.concat(Util.nonNull(message)));
  }

  //~--- get methods ----------------------------------------------------------

  /**
   * Method description
   *
   *
   * @param directory
   * @param name
   *
   * @return
   */
  private File getHookScript(File directory, String name)
  {
    File baseFile = new File(directory,
                      FILE_HOOKDIRECTORY.concat(File.separator).concat(name));

    return IOUtil.getScript(baseFile);
  }

  /**
   * Method description
   *
   *
   * @param rc
   * @param type
   *
   * @return
   */
  private boolean isReceiveable(ReceiveCommand rc, RepositoryHookType type)
  {
    //J-
    return ((RepositoryHookType.PRE_RECEIVE == type) && 
            (rc.getResult() == ReceiveCommand.Result.NOT_ATTEMPTED)) || 
           ((RepositoryHookType.POST_RECEIVE == type) && 
            (rc.getResult() == ReceiveCommand.Result.OK));
    //J+
  }

  /**
   * Method description
   *
   *
   * @param rc
   *
   * @return
   */
  private boolean isUpdateCommand(ReceiveCommand rc)
  {
    return (rc.getType() == ReceiveCommand.Type.UPDATE)
      || (rc.getType() == ReceiveCommand.Type.UPDATE_NONFASTFORWARD);
  }

  //~--- fields ---------------------------------------------------------------

  /** Field description */
  private GitRepositoryHandler handler;

  /** Field description */
  private RepositoryManager repositoryManager;
}
