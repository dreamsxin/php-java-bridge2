/*-*- mode: C; tab-width:4 -*-*/

/** The launcher.exe starts a PHP FastCGI server on Windows.

  Copyright (C) 2003-2007 Jost Boekemeier

  This file is part of the PHP/Java Bridge.

  The PHP/Java Bridge ("the library") is free software; you can
  redistribute it and/or modify it under the terms of the GNU General
  Public License as published by the Free Software Foundation; either
  version 2, or (at your option) any later version.

  The library is distributed in the hope that it will be useful, but
  WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
  General Public License for more details.

  You should have received a copy of the GNU General Public License
  along with the PHP/Java Bridge; see the file COPYING.  If not, write to the
  Free Software Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA
  02111-1307 USA.

  Linking this file statically or dynamically with other modules is
  making a combined work based on this library.  Thus, the terms and
  conditions of the GNU General Public License cover the whole
  combination.

  As a special exception, the copyright holders of this library give you
  permission to link this library with independent modules to produce an
  executable, regardless of the license terms of these independent
  modules, and to copy and distribute the resulting executable under
  terms of your choice, provided that you also meet, for each linked
  independent module, the terms and conditions of the license of that
  module.  An independent module is a module which is not derived from
  or based on this library.  If you modify this library, you may extend
  this exception to your version of the library, but you are not
  obligated to do so.  If you do not wish to do so, delete this
  exception statement from your version. */

/*
 * Compile this program with: i386-pc-mingw32-gcc launcher.c -o
 * launcher.exe -lws2_32
 */

#include <winsock2.h>
#include <windows.h>
#include <stdio.h>
#include <string.h>
#include <stdlib.h>

void die(int line) {
  fprintf(stderr, "launcher.exe terminated with error code %d in line %d.\n", GetLastError(), line);
  exit(2);
}
void usage() {
  puts("This file is part of the PHP/Java Bridge.");
  puts("Copyright (C) 2003, 2006 Jost Boekemeier and others.");
  puts("This is free software; see the source for copying conditions.  There is NO");
  puts("warranty; not even for MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.");
  puts("Usage: launcher.exe processFlags php-cgi.exe php-options");
  puts("processFlags is a dword which is passed to the process, e.g.: CREATE_BREAKAWAY_FROM_JOB");
  puts("");
  puts("Influential environment variables: PHP_FCGI_MAX_REQUESTS, PHP_FCGI_CHILDREN");
  puts("");
  puts("Example, which waits for stdin: launcher.exe 16777216 php-cgi -b 127.0.0.1:9667 -d allow_url_include=On");
  puts("Another example, which waits for Control-C: launcher.exe");
  exit(1);
}

static HANDLE *processes;
static SECURITY_ATTRIBUTES sa = { 0 };
static STARTUPINFO su_info;
static char cmd[8192], *pEnv;
static char buf[255];
static int children = 1;
static short sigkill;

HANDLE start_proc(char*cmd, char*flags) {
  PROCESS_INFORMATION p;
  DWORD pflags;
  
  sscanf(flags, "%lu", &pflags);

  if(!(CreateProcess(NULL, cmd, NULL, NULL, FALSE, pflags, pEnv, NULL, &su_info, &p))) die(__LINE__);
  return p.hProcess;
}

int wait_stdin(void*dummy) {
  char buf[256];
  fgets(buf, sizeof buf, stdin);
  SetEvent(processes[0]);
  return 0;
}

void kill_all_children() {
    register int i;
	for(i=1; i<=children; i++) TerminateProcess(processes[i], 9);
}

BOOL on_exit(DWORD dummy) {
	register int i;
	sigkill = 1;

	kill_all_children();

	return TRUE;
}

int main(int argc, char **argv) {
  extern char **environ;
  HANDLE sem;
  int i, pterm;
  size_t len, envlen;
  char *tmp, **envp, *s, *t;

  if(argc == 2) usage();
  else if(argc<=1) {
	static char *std_argv[] = {NULL, "0", "php-cgi", "-b", "9667"};
	argc = 5;
	argv = std_argv;
  }
								/* windows requires a null terminated
								   block of null terminated env
								   strings */
  for(envp=environ,envlen=0; *envp; envp++) envlen+=1+strlen(*envp);
  envlen++;

  pEnv = malloc(envlen+1);
  for(t=pEnv,envp=environ; *envp; envp++)
    for(s=*envp; *t++=*s++; )
      ;
  *t=0;

  processes = malloc(1+(children*sizeof*processes));
  if(!processes) abort();
  
  
								/* set up process info */
  sa.bInheritHandle = TRUE;
  sa.nLength = sizeof(sa);

  su_info.cb = sizeof(STARTUPINFO);
  su_info.dwFlags = STARTF_USESHOWWINDOW | STARTF_USESTDHANDLES;
  su_info.wShowWindow = SW_HIDE;
  su_info.hStdInput = INVALID_HANDLE_VALUE;
  su_info.hStdError	= INVALID_HANDLE_VALUE;
  su_info.hStdOutput = INVALID_HANDLE_VALUE;
  
								/* the command line */
  for(*cmd=len=0, i=2, argc-=2; argc--; i++) {
    len += 3+strlen(argv[i]);
    if(len>=sizeof(cmd)) abort();
    strcat(cmd, "\"");
    strcat(cmd, argv[i]);
    strcat(cmd, "\"");
    if(argc) strcat(cmd, " ");
  }

//  fputs(cmd, stderr);

								/* start the children */
  if(!(processes[0] = CreateEvent(NULL, TRUE, FALSE, NULL))) die(__LINE__);
  if(argv[0]) CreateThread(NULL, 0, wait_stdin, NULL, 0, NULL);
  SetConsoleCtrlHandler((PHANDLER_ROUTINE)on_exit, TRUE);
  for(i=1; i<=children; i++)
	processes[i]=start_proc(cmd, argv[1]);

								/* until stdin is not available anymore */
  while(1) {
    if((pterm=WaitForMultipleObjects(children+1,processes,FALSE,INFINITE))==WAIT_FAILED) 
	  die(__LINE__);

    if (sigkill) return 2;

    pterm-=WAIT_OBJECT_0;
	if(pterm)
	  processes[pterm]=start_proc(cmd, argv[1]);
	else {
	  kill_all_children();
	  return 0;
	}
  }
  return 3;
}
