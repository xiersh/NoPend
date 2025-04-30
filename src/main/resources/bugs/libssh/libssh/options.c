/*
 * options.c - handle pre-connection options
 *
 * This file is part of the SSH Library
 *
 * Copyright (c) 2003-2008 by Aris Adamantiadis
 * Copyright (c) 2009      by Andreas Schneider <mail@cynapses.org>
 *
 * The SSH Library is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or (at your
 * option) any later version.
 *
 * The SSH Library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with the SSH Library; see the file COPYING.  If not, write to
 * the Free Software Foundation, Inc., 59 Temple Place - Suite 330, Boston,
 * MA 02111-1307, USA.
 */

#include "config.h"
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#ifndef _WIN32
#include <pwd.h>
#else
#include <winsock2.h>
#endif
#include <sys/types.h>
#include "libssh/priv.h"
#include "libssh/session.h"
#include "libssh/misc.h"
#ifdef WITH_SERVER
#include "libssh/server.h"
#endif

/**
 * @brief Duplicate the options of a session structure.
 *
 * If you make several sessions with the same options this is useful. You
 * cannot use twice the same option structure in ssh_session_connect.
 *
 * @param opt           Option structure to copy.
 *
 * @returns New copied option structure, NULL on error.
 *
 * @see ssh_session_connect()
 */
int ssh_options_copy(ssh_session src, ssh_session *dest) {
  ssh_session new;
  int i;

  if (src == NULL || dest == NULL || *dest == NULL) {
    return -1;
  }

  new = *dest;

  if (src->username) {
    new->username = strdup(src->username);
    if (new->username == NULL) {
      return -1;
    }
  }

  if (src->host) {
    new->host = strdup(src->host);
    if (new->host == NULL) {
      return -1;
    }
  }

  if (src->identity) {
    new->identity = strdup(src->identity);
    if (new->identity == NULL) {
      return -1;
    }
  }

  if (src->sshdir) {
    new->sshdir = strdup(src->sshdir);
    if (new->sshdir == NULL) {
      return -1;
    }
  }

  if (src->knownhosts) {
    new->knownhosts = strdup(src->knownhosts);
    if (new->knownhosts == NULL) {
      return -1;
    }
  }

  for (i = 0; i < 10; ++i) {
    if (src->wanted_methods[i]) {
      new->wanted_methods[i] = strdup(src->wanted_methods[i]);
      if (new->wanted_methods[i] == NULL) {
        return -1;
      }
    }
  }

  new->fd = src->fd;
  new->port = src->port;
  new->callbacks = src->callbacks;
  new->timeout = src->timeout;
  new->timeout_usec = src->timeout_usec;
  new->ssh2 = src->ssh2;
  new->ssh1 = src->ssh1;
  new->log_verbosity = src->log_verbosity;

  return 0;
}

#ifndef _WIN32
static char *get_username_from_uid(ssh_session session, uid_t uid){
    struct passwd *pwd = NULL;
    char *name;

    pwd = getpwuid(uid);

    if (pwd == NULL) {
      ssh_set_error(session, SSH_FATAL, "uid %d doesn't exist !", uid);
      return NULL;
    }

    name = strdup(pwd->pw_name);

    if (name == NULL) {
      ssh_set_error_oom(session);
      return NULL;
    }

    return name;
}
#endif

static int ssh_options_set_algo(ssh_session session, int algo,
    const char *list) {
  if (!verify_existing_algo(algo, list)) {
    ssh_set_error(session, SSH_REQUEST_DENIED,
        "Setting method: no algorithm for method \"%s\" (%s)\n",
        ssh_kex_nums[algo], list);
    return -1;
  }

  SAFE_FREE(session->wanted_methods[algo]);
  session->wanted_methods[algo] = strdup(list);
  if (session->wanted_methods[algo] == NULL) {
    ssh_set_error_oom(session);
    return -1;
  }

  return 0;
}

char *dir_expand_dup(ssh_session session, const char *value, int allowsshdir) {
	char *new;

	if (value[0] == '~' && value[1] == '/') {
		char *homedir = ssh_get_user_home_dir();
    size_t lv, lh;

    if (homedir == NULL) {
      return NULL;
    }
    lv = strlen(value + 1);
    lh = strlen(homedir);

		new = malloc(lv + lh + 1);
		if (new == NULL) {
      ssh_set_error_oom(session);
      SAFE_FREE(homedir);
			return NULL;
    }
		memcpy(new, homedir, lh);
    SAFE_FREE(homedir);
		memcpy(new + lh, value + 1, lv + 1);
		return new;
	}
	if (allowsshdir && strncmp(value, "SSH_DIR/", 8) == 0) {
		size_t lv, ls;
		if (session->sshdir == NULL) {
			if (ssh_options_set(session, SSH_OPTIONS_SSH_DIR, NULL) < 0)
				return NULL;
		}

		value += 7;
		lv = strlen(value);
		ls = strlen(session->sshdir);

		new = malloc(lv + ls + 1);
		if (new == NULL) {
      ssh_set_error_oom(session);
			return NULL;
    }
		memcpy(new, session->sshdir, ls);
		memcpy(new + ls, value, lv + 1);
		return new;
	}
  new = strdup(value);
  if (new == NULL) {
    ssh_set_error_oom(session);
    return NULL;
  }
  return new;
}

/**
 * @brief This function can set all possible ssh options.
 *
 * @param  session          An allocated ssh option structure.
 *
 * @param  type         The option type to set. This could be one of the
 *                      following:
 *
 *                      SSH_OPTIONS_HOST:
 *                        The hostname or ip address to connect to (string).
 *
 *                      SSH_OPTIONS_PORT:
 *                        The port to connect to (integer).
 *
 *                      SSH_OPTIONS_PORT_STR:
 *                        The port to connect to (string).
 *
 *                      SSH_OPTIONS_FD:
 *                        The file descriptor to use (socket_t).
 *
 *                        If you wish to open the socket yourself for a reason
 *                        or another, set the file descriptor. Don't forget to
 *                        set the hostname as the hostname is used as a key in
 *                        the known_host mechanism.
 *
 *                      SSH_OPTIONS_USER:
 *                        The username for authentication (string).
 *
 *                        If the value is NULL, the username is set to the
 *                        default username.
 *
 *                      SSH_OPTIONS_SSH_DIR:
 *                        Set the ssh directory (format string).
 *
 *                        If the value is NULL, the directory is set to the
 *                        default ssh directory.
 *
 *                        The ssh directory is used for files like known_hosts
 *                        and identity (private and public key). It may include
 *                        "%s" which will be replaced by the user home
 *                        directory.
 *
 *                      SSH_OPTIONS_KNOWNHOSTS:
 *                        Set the known hosts file name (format string).
 *
 *                        If the value is NULL, the directory is set to the
 *                        default known hosts file, normally ~/.ssh/known_hosts.
 *
 *                        The known hosts file is used to certify remote hosts
 *                        are genuine. It may include "%s" which will be
 *                        replaced by the user home directory.
 *
 *                      SSH_OPTIONS_IDENTITY:
 *                        Set the identity file name (format string).
 *
 *                        By default identity, id_dsa and id_rsa are checked.
 *
 *                        The identity file used authenticate with public key.
 *                        It may include "%s" which will be replaced by the
 *                        user home directory.
 *
 *                      SSH_OPTIONS_TIMEOUT:
 *                        Set a timeout for the connection in seconds (integer).
 *
 *                      SSH_OPTIONS_TIMEOUT_USEC:
 *                        Set a timeout for the connection in micro seconds
 *                        (integer).
 *
 *                      SSH_OPTIONS_SSH1:
 *                        Allow or deny the connection to SSH1 servers
 *                        (integer).
 *
 *                      SSH_OPTIONS_SSH2:
 *                        Allow or deny the connection to SSH2 servers
 *                        (integer).
 *
 *                      SSH_OPTIONS_LOG_VERBOSITY:
 *                        Set the session logging verbosity (integer).
 *
 *                        The verbosity of the messages. Every log smaller or
 *                        equal to verbosity will be shown.
 *                          SSH_LOG_NOLOG: No logging
 *                          SSH_LOG_RARE: Rare conditions or warnings
 *                          SSH_LOG_ENTRY: API-accessible entrypoints
 *                          SSH_LOG_PACKET: Packet id and size
 *                          SSH_LOG_FUNCTIONS: Function entering and leaving
 *
 *                      SSH_OPTTIONS_AUTH_CALLBACK:
 *                        Set a callback to use your own authentication function
 *                        (function pointer).
 *
 *                      SSH_OPTTIONS_AUTH_USERDATA:
 *                        Set the user data passed to the authentication function
 *                        (generic pointer).
 *
 *                      SSH_OPTTIONS_LOG_CALLBACK:
 *                        Set a callback to use your own logging function
 *                        (function pointer).
 *
 *                      SSH_OPTTIONS_LOG_USERDATA:
 *                        Set the user data passed to the logging function
 *                        (generic pointer).
 *
 *                      SSH_OPTTIONS_STATUS_CALLBACK:
 *                        Set a callback to show connection status in realtime
 *                        (function pointer).
 *
 *                        fn(void *arg, float status)
 *
 *                        During ssh_connect(), libssh will call the callback
 *                        with status from 0.0 to 1.0.
 *
 *                      SSH_OPTTIONS_STATUS_ARG:
 *                        Set the status argument which should be passed to the
 *                        status callback (generic pointer).
 *
 *                      SSH_OPTIONS_CIPHERS_C_S:
 *                        Set the symmetric cipher client to server (string,
 *                        comma-separated list).
 *
 *                      SSH_OPTIONS_CIPHERS_S_C:
 *                        Set the symmetric cipher server to client (string,
 *                        comma-separated list).
 *
 *                      SSH_OPTIONS_COMPRESSION_C_S:
 *                        Set the compression to use for client to server
 *                        communication (string, "none" or "zlib").
 *
 *                      SSH_OPTIONS_COMPRESSION_S_C:
 *                        Set the compression to use for server to client
 *                        communication (string, "none" or "zlib").
 *
 * @param  value        The value to set. This is a generic pointer and the
 *                      datatype which is used should be set according to the
 *                      type set.
 *
 * @return              0 on success, < 0 on error.
 */
int ssh_options_set(ssh_session session, enum ssh_options_e type,
    const void *value) {
  char *p, *q;
  long int i;

  if (session == NULL) {
    return -1;
  }

  switch (type) {
    case SSH_OPTIONS_HOST:
      q = strdup(value);
      if (q == NULL) {
        ssh_set_error_oom(session);
        return -1;
      }
      p = strchr(q, '@');

      SAFE_FREE(session->host);

      if (p) {
        *p = '\0';
        session->host = strdup(p + 1);
        if (session->host == NULL) {
          SAFE_FREE(q);
          ssh_set_error_oom(session);
          return -1;
        }

        SAFE_FREE(session->username);
        session->username = strdup(q);
        SAFE_FREE(q);
        if (session->username == NULL) {
          ssh_set_error_oom(session);
          return -1;
        }
      } else {
        session->host = q;
      }
      break;
    case SSH_OPTIONS_PORT:
      if (value == NULL) {
        session->port = 22 & 0xffff;
      } else {
        int *x = (int *) value;

        session->port = *x & 0xffff;
      }
      break;
    case SSH_OPTIONS_PORT_STR:
      if (value == NULL) {
        session->port = 22 & 0xffff;
      } else {
        q = strdup(value);
        if (q == NULL) {
          ssh_set_error_oom(session);
          return -1;
        }
        i = strtol(q, &p, 10);
        if (q == p) {
          SAFE_FREE(q);
        }
        SAFE_FREE(q);

        session->port = i & 0xffff;
      }
      break;
    case SSH_OPTIONS_USER:
      SAFE_FREE(session->username);
      if (value == NULL) { /* set default username */
#ifdef _WIN32
        DWORD size = 0;
        GetUserName(NULL, &size); //Get Size
        q = malloc(size);
        if (q == NULL) {
          ssh_set_error_oom(session);
          return -1;
        }
        if (GetUserName(q, &size)) {
          session->username = q;
        } else {
          SAFE_FREE(q);
          return -1;
        }
#else /* _WIN32 */
        q = get_username_from_uid(session, getuid());
        if (q == NULL) {
          return -1;
        }
        session->username = q;
#endif /* _WIN32 */
      } else { /* username provided */
        session->username = strdup(value);
        if (session->username == NULL) {
          ssh_set_error_oom(session);
          return -1;
        }
      }
      break;
    case SSH_OPTIONS_SSH_DIR:
      if (value == NULL) {
        SAFE_FREE(session->sshdir);
	/* TODO: why ~/.ssh/ instead of ~/.ssh ? */

        session->sshdir = dir_expand_dup(session, "~/.ssh/", 0);
        if (session->sshdir == NULL) {
          return -1;
        }
      } else {
        SAFE_FREE(session->sshdir);
        session->sshdir = dir_expand_dup(session, value, 0);
        if (session->sshdir == NULL) {
          return -1;
        }
      }
      break;
    case SSH_OPTIONS_IDENTITY:

      if (value == NULL) {
        ssh_set_error_invalid(session, __FUNCTION__);
        return -1;
      }
      SAFE_FREE(session->identity);
      session->identity = dir_expand_dup(session, value, 1);
      if (session->identity == NULL) {
        return -1;
      }
      break;
    case SSH_OPTIONS_KNOWNHOSTS:
      if (value == NULL) {
        SAFE_FREE(session->knownhosts);
        session->knownhosts = dir_expand_dup(session,
            "SSH_DIR/known_hosts", 1);
        if (session->knownhosts == NULL) {
          return -1;
        }
      } else {
        SAFE_FREE(session->knownhosts);
        session->knownhosts = dir_expand_dup(session, value, 1);
        if (session->knownhosts == NULL) {
          return -1;
        }
      }
      break;
    case SSH_OPTIONS_TIMEOUT:
      if (value == NULL) {
        ssh_set_error_invalid(session, __FUNCTION__);
        return -1;
      } else {
        long *x = (long *) value;

        session->timeout = *x;
      }
      break;
    case SSH_OPTIONS_TIMEOUT_USEC:
      if (value == NULL) {
        ssh_set_error_invalid(session, __FUNCTION__);
        return -1;
      } else {
        long *x = (long *) value;

        session->timeout_usec = *x;
      }
      break;
    case SSH_OPTIONS_SSH1:
      if (value == NULL) {
        ssh_set_error_invalid(session, __FUNCTION__);
        return -1;
      } else {
        int *x = (int *) value;
        session->ssh1 = *x;
      }
      break;
    case SSH_OPTIONS_SSH2:
      if (value == NULL) {
        ssh_set_error_invalid(session, __FUNCTION__);
        return -1;
      } else {
        int *x = (int *) value;
        session->ssh2 = *x;
      }
      break;
    case SSH_OPTIONS_LOG_VERBOSITY:
      if (value == NULL) {
        ssh_set_error_invalid(session, __FUNCTION__);
        return -1;
      } else {
        int *x = (int *) value;

        session->log_verbosity = *x;
      }
    case SSH_OPTIONS_CIPHERS_C_S:
      if (value == NULL) {
        ssh_set_error_invalid(session, __FUNCTION__);
        return -1;
      } else {
        if (ssh_options_set_algo(session, SSH_CRYPT_C_S, value) < 0)
          return -1;
      }
      break;
    case SSH_OPTIONS_CIPHERS_S_C:
      if (value == NULL) {
        ssh_set_error_invalid(session, __FUNCTION__);
        return -1;
      } else {
        if (ssh_options_set_algo(session, SSH_CRYPT_S_C, value) < 0)
          return -1;
      }
      break;
    case SSH_OPTIONS_COMPRESSION_C_S:
      if (value == NULL) {
        ssh_set_error_invalid(session, __FUNCTION__);
        return -1;
      } else {
        if (ssh_options_set_algo(session, SSH_COMP_C_S, value) < 0)
          return -1;
      }
      break;
    case SSH_OPTIONS_COMPRESSION_S_C:
      if (value == NULL) {
        ssh_set_error_invalid(session, __FUNCTION__);
        return -1;
      } else {
        if (ssh_options_set_algo(session, SSH_COMP_S_C, value) < 0)
          return -1;
      }
      break;
    default:
      ssh_set_error(session, SSH_REQUEST_DENIED, "Unkown ssh option %d", type);
      return -1;
    break;
  }

  return 0;
}

#ifdef WITH_SERVER
static int ssh_bind_options_set_algo(ssh_bind sshbind, int algo,
    const char *list) {
  if (!verify_existing_algo(algo, list)) {
    ssh_set_error(sshbind, SSH_REQUEST_DENIED,
        "Setting method: no algorithm for method \"%s\" (%s)\n",
        ssh_kex_nums[algo], list);
    return -1;
  }

  SAFE_FREE(sshbind->wanted_methods[algo]);
  sshbind->wanted_methods[algo] = strdup(list);
  if (sshbind->wanted_methods[algo] == NULL) {
    ssh_set_error_oom(sshbind);
    return -1;
  }

  return 0;
}

/**
 * @brief This function can set all possible ssh bind options.
 *
 * @param  session      An allocated ssh option structure.
 *
 * @param  type         The option type to set. This could be one of the
 *                      following:
 *
 *                      SSH_BIND_OPTIONS_LOG_VERBOSITY:
 *                        Set the session logging verbosity (integer).
 *
 *                        The verbosity of the messages. Every log smaller or
 *                        equal to verbosity will be shown.
 *                          SSH_LOG_NOLOG: No logging
 *                          SSH_LOG_RARE: Rare conditions or warnings
 *                          SSH_LOG_ENTRY: API-accessible entrypoints
 *                          SSH_LOG_PACKET: Packet id and size
 *                          SSH_LOG_FUNCTIONS: Function entering and leaving
 *
 *                      SSH_BIND_OPTIONS_LOG_VERBOSITY_STR:
 *                        Set the session logging verbosity (integer).
 *
 *                        The verbosity of the messages. Every log smaller or
 *                        equal to verbosity will be shown.
 *                          SSH_LOG_NOLOG: No logging
 *                          SSH_LOG_RARE: Rare conditions or warnings
 *                          SSH_LOG_ENTRY: API-accessible entrypoints
 *                          SSH_LOG_PACKET: Packet id and size
 *                          SSH_LOG_FUNCTIONS: Function entering and leaving
 *
 *                      SSH_BIND_OPTIONS_BINDADDR:
 *                        Set the bind address.
 *
 *                      SSH_BIND_OPTIONS_BINDPORT:
 *                        Set the bind port, default is 22.
 *
 *                      SSH_BIND_OPTIONS_HOSTKEY:
 *                        Set the server public key type: ssh-rsa or ssh-dss
 *                        (string).
 *
 *                      SSH_BIND_OPTIONS_DSAKEY:
 *                        Set the path to the dsa ssh host key (string).
 *
 *                      SSH_BIND_OPTIONS_RSAKEY:
 *                        Set the path to the ssh host rsa key (string).
 *
 *                      SSH_BIND_OPTIONS_BANNER:
 *                        Set the server banner sent to clients (string).
 *
 * @param  value        The value to set. This is a generic pointer and the
 *                      datatype which is used should be set according to the
 *                      type set.
 *
 * @return              0 on success, < 0 on error.
 */
int ssh_bind_options_set(ssh_bind sshbind, enum ssh_bind_options_e type,
    const void *value) {
  char *p, *q;
  int i;

  if (sshbind == NULL) {
    return -1;
  }

  switch (type) {
    case SSH_BIND_OPTIONS_HOSTKEY:
      if (value == NULL) {
        ssh_set_error_invalid(sshbind, __FUNCTION__);
        return -1;
      } else {
        if (ssh_bind_options_set_algo(sshbind, SSH_HOSTKEYS, value) < 0)
          return -1;
      }
      break;
    case SSH_BIND_OPTIONS_BINDADDR:
      if (value == NULL) {
        ssh_set_error_invalid(sshbind, __FUNCTION__);
        return -1;
      } else {
        SAFE_FREE(sshbind->bindaddr);
        sshbind->bindaddr = strdup(value);
        if (sshbind->bindaddr == NULL) {
          ssh_set_error_oom(sshbind);
          return -1;
        }
      }
      break;
    case SSH_BIND_OPTIONS_BINDPORT:
      if (value == NULL) {
        ssh_set_error_invalid(sshbind, __FUNCTION__);
        return -1;
      } else {
        int *x = (int *) value;
        sshbind->bindport = *x & 0xffff;
      }
      break;
    case SSH_BIND_OPTIONS_BINDPORT_STR:
      if (value == NULL) {
        sshbind->bindport = 22 & 0xffff;
      } else {
        q = strdup(value);
        if (q == NULL) {
          ssh_set_error_oom(sshbind);
          return -1;
        }
        i = strtol(q, &p, 10);
        if (q == p) {
          SAFE_FREE(q);
        }
        SAFE_FREE(q);

        sshbind->bindport = i & 0xffff;
      }
      break;
    case SSH_BIND_OPTIONS_LOG_VERBOSITY:
      if (value == NULL) {
        ssh_set_error_invalid(sshbind, __FUNCTION__);
        return -1;
      } else {
        int *x = (int *) value;
        sshbind->log_verbosity = *x & 0xffff;
      }
      break;
    case SSH_BIND_OPTIONS_LOG_VERBOSITY_STR:
      if (value == NULL) {
        sshbind->log_verbosity = 0;
      } else {
        q = strdup(value);
        if (q == NULL) {
          ssh_set_error_oom(sshbind);
          return -1;
        }
        i = strtol(q, &p, 10);
        if (q == p) {
          SAFE_FREE(q);
        }
        SAFE_FREE(q);

        sshbind->log_verbosity = i & 0xffff;
      }
      break;
    case SSH_BIND_OPTIONS_DSAKEY:
      if (value == NULL) {
        ssh_set_error_invalid(sshbind, __FUNCTION__);
        return -1;
      } else {
        sshbind->dsakey = strdup(value);
        if (sshbind->dsakey == NULL) {
          ssh_set_error_oom(sshbind);
          return -1;
        }
      }
      break;
    case SSH_BIND_OPTIONS_RSAKEY:
      if (value == NULL) {
        ssh_set_error_invalid(sshbind, __FUNCTION__);
        return -1;
      } else {
        SAFE_FREE(sshbind->rsakey);
        sshbind->rsakey = strdup(value);
        if (sshbind->rsakey == NULL) {
          ssh_set_error_oom(sshbind);
          return -1;
        }
      }
      break;
    case SSH_BIND_OPTIONS_BANNER:
      if (value == NULL) {
        ssh_set_error_invalid(sshbind, __FUNCTION__);
        return -1;
      } else {
        SAFE_FREE(sshbind->banner);
        sshbind->banner = strdup(value);
        if (sshbind->banner == NULL) {
          ssh_set_error_oom(sshbind);
          return -1;
        }
      }
      break;
    default:
      ssh_set_error(sshbind, SSH_REQUEST_DENIED, "Unkown ssh option %d", type);
      return -1;
    break;
  }

  return 0;
}
#endif

/**
 * @brief Parse command line arguments.
 *
 * This is a helper for your application to generate the appropriate
 * options from the command line arguments.\n
 * The argv array and argc value are changed so that the parsed
 * arguments wont appear anymore in them.\n
 * The single arguments (without switches) are not parsed. thus,
 * myssh -l user localhost\n
 * The command wont set the hostname value of options to localhost.
 *
 * @param session       The session to configure.
 *
 * @param argcptr       The pointer to the argument count.
 *
 * @param argv          The arguments list pointer.
 *
 * @returns 0 on success, < 0 on error.
 *
 * @see ssh_session_new()
 */
int ssh_options_getopt(ssh_session session, int *argcptr, char **argv) {
  char *user = NULL;
  char *cipher = NULL;
  char *localaddr = NULL;
  char *identity = NULL;
  char *port = NULL;
  char *bindport = NULL;
  char **save = NULL;
  int i = 0;
  int argc = *argcptr;
  int debuglevel = 0;
  int usersa = 0;
  int usedss = 0;
  int compress = 0;
  int cont = 1;
  int current = 0;
#ifdef WITH_SSH1
  int ssh1 = 1;
#else
  int ssh1 = 0;
#endif
  int ssh2 = 1;
#ifdef _MSC_VER
    /* Not supported with a Microsoft compiler */
    return -1;
#else
  int saveoptind = optind; /* need to save 'em */
  int saveopterr = opterr;

  save = malloc(argc * sizeof(char *));
  if (save == NULL) {
    ssh_set_error_oom(session);
    return -1;
  }

  opterr = 0; /* shut up getopt */
  while(cont && ((i = getopt(argc, argv, "c:i:Cl:p:vb:t:rd12")) != -1)) {
    switch(i) {
      case 'l':
        user = optarg;
        break;
      case 'p':
        port = optarg;
        break;
      case 't':
        bindport = optarg;
        break;
      case 'v':
        debuglevel++;
        break;
      case 'r':
        usersa++;
        break;
      case 'd':
        usedss++;
        break;
      case 'c':
        cipher = optarg;
        break;
      case 'i':
        identity = optarg;
        break;
      case 'b':
        localaddr = optarg;
        break;
      case 'C':
        compress++;
        break;
      case '2':
        ssh2 = 1;
        ssh1 = 0;
        break;
      case '1':
        ssh2 = 0;
        ssh1 = 1;
        break;
      default:
        {
          char opt[3]="- ";
          opt[1] = optopt;
          save[current] = strdup(opt);
          if (save[current] == NULL) {
            SAFE_FREE(save);
            ssh_set_error_oom(session);
            return -1;
          }
          current++;
          if (optarg) {
            save[current++] = argv[optind + 1];
          }
        }
    } /* switch */
  } /* while */
  opterr = saveopterr;
  while (optind < argc) {
    save[current++] = argv[optind++];
  }

  if (usersa && usedss) {
    ssh_set_error(session, SSH_FATAL, "Either RSA or DSS must be chosen");
    cont = 0;
  }

  ssh_options_set(session, SSH_OPTIONS_LOG_VERBOSITY, &debuglevel);

  optind = saveoptind;

  if(!cont) {
    SAFE_FREE(save);
    return -1;
  }

  /* first recopy the save vector into the original's */
  for (i = 0; i < current; i++) {
    /* don't erase argv[0] */
    argv[ i + 1] = save[i];
  }
  argv[current + 1] = NULL;
  *argcptr = current + 1;
  SAFE_FREE(save);

  /* set a new option struct */
  if (compress) {
    if (ssh_options_set(session, SSH_OPTIONS_COMPRESSION_C_S, "zlib") < 0) {
      cont = 0;
    }
    if (ssh_options_set(session, SSH_OPTIONS_COMPRESSION_S_C, "zlib") < 0) {
      cont = 0;
    }
  }

  if (cont && cipher) {
    if (ssh_options_set(session, SSH_OPTIONS_CIPHERS_C_S, cipher) < 0) {
      cont = 0;
    }
    if (cont && ssh_options_set(session, SSH_OPTIONS_CIPHERS_S_C, cipher) < 0) {
      cont = 0;
    }
  }

  if (cont && user) {
    if (ssh_options_set(session, SSH_OPTIONS_USER, user) < 0) {
      cont = 0;
    }
  }

  if (cont && identity) {
    if (ssh_options_set(session, SSH_OPTIONS_IDENTITY, identity) < 0) {
      cont = 0;
    }
  }

  ssh_options_set(session, SSH_OPTIONS_PORT_STR, port);

  ssh_options_set(session, SSH_OPTIONS_SSH1, &ssh1);
  ssh_options_set(session, SSH_OPTIONS_SSH2, &ssh2);

  if (!cont) {
    return SSH_ERROR;
  }

  return SSH_OK;
#endif
}

/**
 * @brief Parse the ssh config file.
 *
 * This should be the last call of all options, it may overwrite options which
 * are already set. It requires that the host name is already set with
 * ssh_options_set_host().
 *
 * @param  session      SSH session handle
 *
 * @param  filename     The options file to use, if NULL the default
 *                      ~/.ssh/config will be used.
 *
 * @return 0 on success, < 0 on error.
 *
 * @see ssh_options_set_host()
 */
int ssh_options_parse_config(ssh_session session, const char *filename) {
  char *expanded_filename;
  int r;

  if (session == NULL) {
    return -1;
  }
  if (session->host == NULL) {
    ssh_set_error_invalid(session, __FUNCTION__);
    return -1;
  }

  /* set default filename */
  if (filename == NULL) {
    expanded_filename = dir_expand_dup(session, "SSH_DIR/config", 1);
  } else {
    expanded_filename = dir_expand_dup(session, filename, 1);
  }
  if (expanded_filename == NULL)
    return -1;

  r = ssh_config_parse_file(session, expanded_filename);
  free(expanded_filename);
  return r;
}

/** @} */
/* vim: set ts=2 sw=2 et cindent: */
