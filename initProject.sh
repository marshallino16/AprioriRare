#!/bin/bash
if [ -f AprioriRareMining.class ]; then
    echo "File found"
    rm AprioriRareMining.class
fi
echo "[+] compiling"
javac AprioriRareMining.java #AprioriRareMining
echo "[+] launching"
java AprioriRareMining chess.dat out.txt 0.3
