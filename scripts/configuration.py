import os

METHODS = ["SOFOT", "FIC", "FICBS", "BEN", "IterAIFL", "TRT", "ComFIL", "NoPend"]
SUTS = ["dia", "apache1", "busybox", "linux", "vim1", "apache2", "gnuplot", "bash1", "vim2", "vim3", "bash2", "xterm"]
KEY_LISTS = ['time', 'numAT_avg', 'precision', 'recall', 'pendingSchemaProportion']
CSV_LISTS = ['time', 'numAT_avg', 'pr_with_mean', "pending_with_mean_percentage", "pr_with_mean_percentage"]
SUBJECTS_DICT = {
    r"bugs/dia/lib/prop_text.c": r"dia",
    r"bugs/apache/modules/slotmem/mod_slotmem_shm.c": r"apache1", 
    r"bugs/busybox/networking/ftpd.c": r"busybox", 
    r"bugs/linux/init/main.c": r"linux", 
    r"bugs/vim/src/netbeans.c": r"vim1", 
    r"bugs/apache/os/unix/unixd.c": r"apache2", 
    r"bugs/gnuplot/src/plot.c": r"gnuplot", 
    r"bugs/bash/execute_cmd.c": r"bash1", 
    r"bugs/vim/src/ex_cmds.c": r"vim2", 
    r"bugs/vim/src/os_unix.c": r"vim3", 
    r"bugs/bash/support/getcppsyms.c": r"bash2", 
    r"bugs/xterm/main.c": r"xterm"
}
REPEAT_NUM = 10

RESULTS_DIR = "evaluation_results" # replcae with your own results directory
TEX_DIR = "tex"
os.makedirs(RESULTS_DIR, exist_ok=True)
os.makedirs(TEX_DIR, exist_ok=True)