#!/bin/bash

# Written by Evan Downing

# Check for correct number of parameters
if [[ $# -ne 1 ]]; then
    echo "usage: ./load-new.sh malware-folder"
    exit 1
fi

MALWARE=`ls -1 $1`
location=/opt/gtisc/nvmtrace/input

for m in $MALWARE;
do
    # Calculate SHA256
    sha=`sha256sum -b $1/$m | awk '{print $1}'`

    # Query database to see if sha2565 entry exists in table
    out=`psql -d nvmtrace -At -c "SELECT COUNT(*) FROM sample WHERE sha256='$sha';"`

    # If query does not exist, insert it into the table
    if [[ $out -eq 0 ]]; then
        # Get timestamp of executable (in UNIX time)
        time=`stat -c %Y $1/$m`

        # Prepare query to insert malware record into table
        query=`printf "INSERT INTO sample VALUES('%s',%s,NULL);" "$sha" "$time"`

        # Insert malware record into table
        out=`psql -d nvmtrace -At -c "$query"`
        echo "$m: $out"

        # Copy malware executable into input location for nvmtrace to retrieve
        sudo cp $1/$m $location/$sha
    fi
done
