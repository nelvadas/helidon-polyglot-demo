#include <stdio.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <unistd.h>


int main() {
    char *filename="/tmp/covid-data.csv";
    printf("Checking the downloaded file!\n");
    struct stat st = {0};
    int status=stat(filename, &st);
    printf("%s file attributes : size: %lld bytes, last modified: %ld \n",filename, st.st_size, st.st_mtime);
    return status;
}
