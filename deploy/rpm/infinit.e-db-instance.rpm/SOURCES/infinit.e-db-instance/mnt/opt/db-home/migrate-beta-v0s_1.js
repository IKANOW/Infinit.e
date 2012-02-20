//use admin;
db.runCommand({renameCollection:"file.jars.chunks",to:"file.binary_shares.chunks"});
db.runCommand({renameCollection:"file.jars.files",to:"file.binary_shares.files"});
db.runCommand({renameCollection:"admin.uisetup",to:"gui.setup"});
db.runCommand({renameCollection:"module.modules",to:"gui.modules"});
db.runCommand({renameCollection:"module.usermodules",to:"gui.favmodules"});
db.runCommand({renameCollection:"harvester.sources",to:"config.source"});
db.runCommand({renameCollection:"harvester.feeds",to:"doc_metadata.metadata"});
db.runCommand({renameCollection:"harvester.gzip_content",to:"doc_content.gzip_content"});
db.runCommand({renameCollection:"harvester.georeference",to:"feature.geo"});
db.runCommand({renameCollection:"harvester.gazateer",to:"feature.entity"});
db.runCommand({renameCollection:"harvester.event_gazateer",to:"feature.event"});
//template:
//db.runCommand({renameCollection:"sourcedb.mycol",to:"targetdb.mycol"});