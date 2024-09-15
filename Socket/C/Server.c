// server.c
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <arpa/inet.h>
#include <time.h>
#include <sys/types.h>
#include <sys/socket.h>
#include <sys/wait.h>

#define PORT 8080
#define BACKLOG 5

void handle_client(int client_sock) {
    char buffer[256];
    time_t rawtime;
    struct tm *timeinfo;

    while (1) {
        // Get the current time
        time(&rawtime);
        timeinfo = localtime(&rawtime);

        // Format the time as a string
        snprintf(buffer, sizeof(buffer), "Current date and time: %s", asctime(timeinfo));

        // Send the time to the client
        send(client_sock, buffer, strlen(buffer), 0);

        // Sleep for 1 second before sending the next update
        sleep(1);
    }

    close(client_sock);
}

int main() {
    int server_sock, client_sock;
    struct sockaddr_in server_addr, client_addr;
    socklen_t addr_len = sizeof(client_addr);
    pid_t pid;

    // Create TCP socket
    server_sock = socket(AF_INET, SOCK_STREAM, 0);
    if (server_sock == -1) {
        perror("Socket creation failed");
        exit(EXIT_FAILURE);
    }

    // Configure server address
    server_addr.sin_family = AF_INET;
    server_addr.sin_addr.s_addr = INADDR_ANY; // Bind to any available interface
    server_addr.sin_port = htons(PORT); // Use port 8080

    // Bind the socket
    if (bind(server_sock, (struct sockaddr *)&server_addr, sizeof(server_addr)) == -1) {
        perror("Bind failed");
        close(server_sock);
        exit(EXIT_FAILURE);
    }

    // Listen for incoming connections
    if (listen(server_sock, BACKLOG) == -1) {
        perror("Listen failed");
        close(server_sock);
        exit(EXIT_FAILURE);
    }

    printf("Server is listening on port %d...\n", PORT);

    while (1) {
        // Accept incoming connection
        client_sock = accept(server_sock, (struct sockaddr *)&client_addr, &addr_len);
        printf("Accepted connection from %s:%d\n", inet_ntoa(client_addr.sin_addr), ntohs(client_addr.sin_port));
        if (client_sock == -1) {
            perror("Accept failed");
            continue;
        }

        printf("Accepted connection from %s:%d\n", inet_ntoa(client_addr.sin_addr), ntohs(client_addr.sin_port));

        // Fork a new process to handle the client
        pid = fork();
        if (pid == 0) {
            // Child process handles client
            close(server_sock);
            handle_client(client_sock);
            exit(0);
        } else if (pid > 0) {
            // Parent process: close the client socket
            printf("Parent process closing client socket at %d\n", client_sock);
            close(client_sock);
            wait(NULL); // Optionally wait for child processes to prevent zombies
        } else {
            perror("Fork failed");
        }
    }

    close(server_sock);
    return 0;
}
