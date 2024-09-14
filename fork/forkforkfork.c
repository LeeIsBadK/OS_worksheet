// compile: gcc fork.c -o fork
// run: ./fork

#include <stdio.h>
#include <stdlib.h>   // Include this for the exit() function
#include <sys/types.h>
#include <sys/wait.h> // Include this for wait() function
#include <unistd.h>   // Include this for fork() and getpid()

int main() {
    const int process = 10;  // Number of child processes to create
    int parent = (int) getpid();  // Parent's process ID

    for (int i = 0; i < process; i++) {
        pid_t pid = fork();  // Create a new child process

        if (pid < 0) {
            // Fork failed
            printf("Fork failed\n");
            exit(1);
        } else if (pid == 0) {
            // Child process
            printf("I'm child number %d (pid %d)\n", i + 1, (int) getpid());
            exit(0);  // Child process exits
        }
    }

    // Parent waits for all child processes to finish
    int status;
    for (int i = 0; i < process; ++i) {
        wait(&status);
    }

    // Parent process terminates
    printf("Parent terminates (pid %d)\n", parent);

    return 0;
}

