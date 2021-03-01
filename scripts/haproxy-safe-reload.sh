#!/bin/bash

/usr/sbin/haproxy -c -V -f /etc/haproxy/haproxy.cfg > /dev/null

retVal=$?
if [ $retVal -ne 0 ]; then
	echo "haproxy could not be reloaded, syntax check failed"
else 
	# reload haproxy
	service haproxy reload
fi

exit $retVal
