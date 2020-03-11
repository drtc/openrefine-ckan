# OpenRefine CKAN Export Extension

Directly upload from OpenRefine to CKAN using CKAN API.

Currently tested and works with OpenRefine 3.3 and CKAN v2.8.

This project is a modifed and updated version of [OpenGov](https://github.com/OpenGov-OpenData/openrefine-ckan-storage-extension)
 package, which was based on [Fadmaa](https://github.com/fadmaa/grefine-ckan-storage-extension)'s work. 


You can find more about the earlier versions here:

[https://github.com/OpenGov-OpenData/openrefine-ckan-storage-extension](https://github.com/OpenGov-OpenData/openrefine-ckan-storage-extension)

and

[https://github.com/fadmaa/grefine-ckan-storage-extension](https://github.com/fadmaa/grefine-ckan-storage-extension)


Installation
-----
Binary package not yet ready. Only source available.

Developers
-----
* Download or clone OpenRefine 3.3 or latest
* Clone openrefine-ckan source into OpenRefine's extensions folder:
        
        git clone https://github.com/OpenRefine/OpenRefine.git
        cd OpenRefine/extensions
        git clone <this-repository>

* Add the following line to OpenRefine/extensions/pom.xml (under <modules>):

        <modules>
            ...
            <module>openrefine-ckan</module>
        <modules>
        
* Recompile OpenRefine source code as:

        ./refine clean
        ./refind build
        
* Start OpenRefine

        ./refine

TODO
-----
*   Create binary package
*   Feel free to contribute
