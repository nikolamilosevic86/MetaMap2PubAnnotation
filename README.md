# MetaMap2PubAnnotation
MetaMap2PubAnnotation is a tool that is using MetaMap API to connect to MetaMap, tag supplied text documents with UMLS concept IDs and generate JSON files in PubAnnotation format.   

Tool is written in Java and depends on it. 

## How to run
The tool uses 4 command line parameters:
* host of the MetaMap server
* port number - default port number for metamap server is 8066, so you can use that number
* directory with files. The tool with use filenames as sourceid in PubAnnotation format
* Name of sourcedb - usually PMC or PubMed