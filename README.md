# Google Refine CKAN Storage Extension
# (for CKAN v2.2+)

Upload data directly from Google Refine to CKAN using CKAN API.

This project is a modifed and updated version of [Fadmaa](https://github.com/fadmaa/grefine-ckan-storage-extension)'s work. The earlier version did not support CKAN API v3. The source code is updated to comply with CKAN 2.2 and API v3. 

You can find more about the earlier extension version here:
[http://lab.linkeddata.deri.ie/2011/grefine-ckan/](http://lab.linkeddata.deri.ie/2011/grefine-ckan/)
and
[https://github.com/fadmaa/grefine-ckan-storage-extension](https://github.com/fadmaa/grefine-ckan-storage-extension)


Installation
-----
* Make sure you have Google Refine installed on your machine (see [here](https://github.com/OpenRefine/OpenRefine))
* Pull grefine-ckan-storage-extension source into OpenRefine's extensions folder::
        cd [[path_to_refine]]/OpenRefine/extensions/
        git clone https://github.com/Ontodia/grefine-ckan-storage-extension.git
* Restart Refine
         cd [[path_to_refine]]/OpenRefine/
        ./refine

Developers
-----
* If you made any changes to the original source. Recompile build.xml with ant:
        cd [[path_to_refine]]/OpenRefine/grefine-ckan-storage-extension/
        ant clean
        ant build
* Restart Refine


TODO
-----
*   Currently the upload uses CKAN API [create_resource](http://docs.ckan.org/en/latest/api/#ckan.logic.action.create.resource_create) action. By defualt this will timeout for large files after 30 secs. In order to fix this; use [datastore api](http://docs.ckan.org/en/ckan-2.2/datastore.html), split data into chunks and upload chunks via datastore api.
*   Better documentation.
*   Code clean-up
