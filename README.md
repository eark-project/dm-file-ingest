dm-file-ingest
======

eArk WP6 - index file contents from extracted archives

Text is extracted from PDF-, Word and other documents. Structural information (e.g. headlines) is not parsed and can not be used for search queries.

# How to: reset the Lily index and/or add new fields

reset lily index
----------------
cd /srv/lily-2.4/bin

# list indexes
./lily-list-indexes

# set environment
LILY_CONFIG=/srv/dm/dm-file-ingest/src/main/config/lily

# only if a new field should be added: edit the following files
$LILY_CONFIG/schema.json
$LILY_CONFIG/indexerconf.xml
/srv/apache-solr-4.0.0/example/solr/eark1/conf/schema.xml

# load the schema
./lily-import -s $LILY_CONFIG/schema.json

# delete the now outdated index
./lily-update-index -n eark1 --state DELETE_REQUESTED

# add index
./lily-add-index -n eark1 -c $LILY_CONFIG/indexerconf.xml -sm classic -s shard1:http://localhost:8983/solr/eark1 -dt eark1

# clear solr index
curl http://localhost:8983/solr/eark1/update/?commit=true -d "<delete><query>*:*</query></delete>" -H "Content-Type: text/xml"

# rebuild the index
./lily-update-index -n eark1 --build-state BUILD_REQUESTED