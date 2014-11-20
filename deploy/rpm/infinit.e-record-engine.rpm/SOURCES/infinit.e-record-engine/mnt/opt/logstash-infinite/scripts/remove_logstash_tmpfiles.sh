#
# Workaround for unclean logstash shutdown issues 
# 
# Check tmpdir ... if nothing in the temp logstash directories are being accessed then delete them

TMPDIR="/mnt/tmp" && [[ -d /raidarray ]] && TMPDIR="/raidarray/tmp"

for i in $(find $TMPDIR -name "logstash*"); do
	if [ -d $i ]; then
		for j in $(find $i -type f); do
			lsof "$j" > /dev/null || rm -f $j
		done
		if [ ! -n "$(ls -A $i)" ]; then
			rmdir $i > /dev/null
		fi
	else
		lsof "$i" > /dev/null || rm -f $i
	fi
done
#(TESTED)

#Also, check all the temp .sincedb files are writable by everyone, otherwise this can cause massive issues
if [ -d /opt/logstash-infinite/logstash/ ]; then
        chmod a+w /opt/logstash-infinite/logstash/.sincedb*
fi

#Ditto for the .conf files, though this will cause less carnage
if [ -d /opt/logstash-infinite/logstash.conf.d/ ]; then
        chmod a+w /opt/logstash-infinite/logstash.conf.d/*.conf
fi
if [ -d /opt/logstash-infinite/dist.logstash.conf.d/ ]; then
        chmod a+w /opt/logstash-infinite/dist.logstash.conf.d/*.conf
fi
