#!/bin/bash

# Written by Evan Downing

# Check for correct number of parameters
if [[ $# -ne 1 ]]; then
    echo "usage: ./load.sh sha256-malware-folder"
    exit 1
fi

MALWARE=`ls -1 $1`
location=/opt/gtisc/nvmtrace/input

for m in $MALWARE;
do
    # Query database to see if sha2565 entry exists in table
    out=`psql -d nvmtrace -At -c "SELECT COUNT(*) FROM sample WHERE sha256='$m';"`

    # If query exists, change it's process_time in the table
    if [[ $out -ne 0 ]]; then
        # Prepare query to change malware record in table
        query=`printf "UPDATE sample SET process_time=NULL WHERE sha256='$m';"`

        out=`psql -d nvmtrace -At -c "$query"`
        echo "$m: $out"
    # Else insert new sample
    else
        # Get timestamp of executable (in UNIX time)
        time=`stat -c %Y $1/$m`

        # Prepare query to insert malware record into table
        query=`printf "INSERT INTO sample VALUES('%s',%s,NULL);" "$m" "$time"`

        # Insert malware record into table
        out=`psql -d nvmtrace -At -c "$query"`
        echo "$m: $out"
    fi

    # Copy malware executable into input location for nvmtrace to retrieve
    sudo cp $1/$m $location
done
