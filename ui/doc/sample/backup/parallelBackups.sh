#!/bin/bash        

##########################################################################################
# This file acts as a wrapper to allow parallel execution of global backups.
##########################################################################################
# Parameters:
#	-v	Verbose output
#	-n	Max child processes
##########################################################################################
VERSION=1
DFORMAT="%y%m%d %T"

##########################################################################################
# This is the maximum number of concurrent processes
# This is to be tuned depending on the hardware characteristics of the host machine
# Take note that HDD performance might have a huge impact here...
##########################################################################################
MAX_CHILDREN=5

##########################################################################################
# PHP executable to use
##########################################################################################
PHP=/usr/bin/php

##########################################################################################
# Main backup file to invoke
##########################################################################################
PHP_BACKUP_FILE=/usr/share/obm/www/cron/ManualGlobalBackup.class.php

##########################################################################################
# Descriptor file generated by the PHP side during --init-backup
##########################################################################################
DESCRIPTOR_FILE=/tmp/globalBackups.init

##########################################################################################
# Verbose output?
##########################################################################################
VERBOSE=false

function log() {
	local level=$1; shift;
	
	echo "[`date +"$DFORMAT"`][$level	] - $*"
}

function logError() {
	log "ERROR" $*
}

function logInfo() {
	log "INFO" $*
}

function logVerbose() {
	if $VERBOSE
	then
		log "DEBUG" $*
	fi
}

function logHeader() {
	logInfo "--------------------------------------------------------------------------------"
}

function logWithHeader() {
	logHeader
	logInfo $*
	logHeader
}

function endScript() {
	logWithHeader "Done! (Returning $1)"
	exit $1
}

function callPhp() {
	logVerbose "Executing '$PHP $PHP_BACKUP_FILE $*'"
	
	$PHP $PHP_BACKUP_FILE $*
}

function initBackup() {
	logInfo "Initializing backup"
	
	callPhp --init-backup
	
	if [ ! -e $DESCRIPTOR_FILE ]
	then
		logError "Descriptor file '$DESCRIPTOR_FILE' doesn't exist, check PHP log files"
		endScript 2
	fi
}

function processDescriptorFile() {
	logInfo "Loading backup descriptor file '$DESCRIPTOR_FILE'"
	
	while read line
	do
		local domainId=`echo "$line" | cut -d= -f1`
		local nbUsersByBackendInDomain=`echo "$line" | cut -d= -f2`
		
		logInfo "Processing domain $domainId"
		
		for nbUsersByBackend in $nbUsersByBackendInDomain
		do
			local backendId=`echo $nbUsersByBackend | cut -d: -f1`
			local nbUsersInBackend=`echo $nbUsersByBackend | cut -d: -f2`
			
			processDomainBackend $domainId $backendId $nbUsersInBackend
		done
		
		waitForChildren
	done < $DESCRIPTOR_FILE
	
	logInfo "Done processing descriptor file '$DESCRIPTOR_FILE'"
}

function processDomainBackend() {
	local domainId=$1
	local backendId=$2
	local nbUsersInBackend=$3
	local nbUsersPerProcess=$(($nbUsersInBackend / $MAX_CHILDREN))
	local nbUsersForLastProcess=$(($nbUsersPerProcess + $nbUsersInBackend - ($MAX_CHILDREN * $nbUsersPerProcess)))
	local nbSpawnedChildren=0
		
	logInfo "Processing backend $backendId (for domain $domainId) containing $nbUsersInBackend user(s)"
	
	# All but the last child use nbUsersPerProcess
	while [ $nbSpawnedChildren -ne $(($MAX_CHILDREN - 1)) ]
	do
		if [ $nbUsersPerProcess -ne 0 ]
		then
			forkChild $domainId $backendId $(($nbSpawnedChildren * $nbUsersPerProcess)) $nbUsersPerProcess
		fi
		nbSpawnedChildren=$(($nbSpawnedChildren + 1))
	done
	
	# The last child uses the remaining number of users
	forkChild $domainId $backendId $((($MAX_CHILDREN - 1) * $nbUsersPerProcess)) $nbUsersForLastProcess
}

function forkChild() {
	logVerbose "Forking backup process for domain $1 (backend $2), backing up $4 user(s) starting from $3"
	
	callPhp --backup --domain=$1 --backend=$2 --offset=$3 --nbUsers=$4 &
	pIds="$pIds $!"
}

function waitForChildren() {
	logInfo "Waiting for `echo "$pIds" | wc -w` child process(es) to complete"
	
	for pId in $pIds
	do
		wait $pId
	done
	# Clear out pIds so that we can safely process another domain
	pIds=""
	
	logInfo "All child processes exited"
}

function endOfBackup() {
	logInfo "Notifying the end of the backup"
	
	callPhp --end-backup
}

function main() {
	logInfo "Initializing execution (using $MAX_CHILDREN maximum forks)"
	
	initBackup
	processDescriptorFile
	endOfBackup
	
	endScript 0
}

# Main execution

logWithHeader "Parallel global backups wrapper script v$VERSION"

args=`getopt :vn: $*`
if test $? != 0
	then
		logError "Wrong usage! Available options are:"
		logError "	-v	Verbose output"
		logError "	-n	Maximum number of child processes to use"
        endScript 1
fi

set -- $args
for i
do
  case "$i" in
        -v) shift; VERBOSE=true;;
        -n) shift; MAX_CHILDREN=$1;;
  esac
done

main
