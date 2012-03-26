#!/bin/bash
NAME=CentOS_5.5_X86_64_V6_GOLD
SNAPID=snap-INSERT_SNAPSHOT_ID
KERNELID=aki-9800e5f1
KEYFILE=pk-aws.pem
CERTFILE=cert-aws.pem
#
ec2-register -n "$NAME"  -d "Gold Centos 5.5 with Java, runs user-data as script on startup" \
	-a x86_64 --root-device-name '/dev/sda1' --block-device-mapping /dev/sda1=$SNAPID \
		--block-device-mapping /dev/sdb=ephemeral0 --block-device-mapping /dev/sdc=ephemeral1 \ 
		--block-device-mapping /dev/sdd=ephemeral2 --block-device-mapping /dev/sde=ephemeral3 \
	--kernel $KERNELID \
	-K $KEYFILE -C $CERTFILE
