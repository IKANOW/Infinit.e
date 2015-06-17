if [ "$(id -u -n)" = "tomcat" ] || [ "$(id -u -n)" = "logstash" ]; then
		#(logstash user doesn't get applied in practice - have also edited /etc/sysconfig/logstash)
        if [ -d /raidarray ]; then
                TMPDIR=/raidarray/tmp
				export TMPDIR
        else
            	TMPDIR=/mnt/tmp
				export TMPDIR
        fi
fi
