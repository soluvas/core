/*
 * JBoss, Home of Professional Open Source
 * Copyright 2011, Red Hat, Inc., and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.forge.shell.util;

import java.io.IOException;
import java.io.InputStream;

import org.jboss.forge.resources.DirectoryResource;
import org.jboss.forge.shell.ShellPrintWriter;

/**
 * Executes native system commands.
 * 
 * @author <a href="mailto:lincolnbaxter@gmail.com">Lincoln Baxter, III</a>
 * 
 */
public class NativeSystemCall
{
   /**
    * Execute a native system command as if it were run from the given path.
    * 
    * @param command the system command to execute
    * @param parms the command parameters
    * @param out a print writer to which command output will be streamed
    * @param path the path from which to execute the command
    * 
    * @return 0 on successful completion, any other return code denotes failure
    */
   public static int execFromPath(final String command, final String[] parms, final ShellPrintWriter out,
            final DirectoryResource path) throws IOException
   {
      try
      {
         String[] commandTokens = parms == null ? new String[1] : new String[parms.length + 1];
         commandTokens[0] = command;

         if (commandTokens.length > 1)
         {
            System.arraycopy(parms, 0, commandTokens, 1, parms.length);
         }

         ProcessBuilder builder = new ProcessBuilder(commandTokens);
         builder.directory(path.getUnderlyingResourceObject());
         builder.redirectErrorStream(true);
         Process p = builder.start();

         InputStream stdout = p.getInputStream();

         Thread outThread = new Thread(new Receiver(stdout, out));
         outThread.start();
         outThread.join();

         return p.waitFor();

      }
      catch (InterruptedException e)
      {
         e.printStackTrace();
         return -1;
      }
   }

   /**
    * Execute the given system command
    * 
    * @return 0 on successful completion, any other return code denotes failure
    */
   public static void exec(final boolean wait, final String command, final String... parms)
            throws IOException
   {
      String[] commandTokens = parms == null ? new String[1] : new String[parms.length + 1];
      commandTokens[0] = command;

      if (commandTokens.length > 1)
      {
         System.arraycopy(parms, 0, commandTokens, 1, parms.length);
      }

      Runtime.getRuntime().exec(commandTokens, null);
   }

   /**
    * Handles streaming output from executed Processes
    */
   private static class Receiver implements Runnable
   {
      private final InputStream in;
      private final ShellPrintWriter out;

      public Receiver(InputStream in, ShellPrintWriter out)
      {
         this.in = in;
         this.out = out;
      }

      @Override
      public void run()
      {
         try {
            byte[] buf = new byte[10];
            int read;
            while ((read = in.read(buf)) != -1)
            {
               for (int i = 0; i < read; i++)
               {
                  out.write(buf[i]);
               }
            }
         }
         catch (IOException e) {
            throw new RuntimeException("Error reading input from child process", e);
         }

      }
   }
}
