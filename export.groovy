resolveHierarchy()
selectObjectsByClassification("Bad")
clearSelectedObjects(false)
selectObjectsByClassification(null)
def newPathClass = getPathClass("Clone") 
getSelectedObjects().forEach {
    it.setPathClass(newPathClass)
}

resolveHierarchy()

server = getCurrentImageData().getServer()
x = server.getPixelCalibration()
print server.getPixelCalibration()

// Get the 'project image entry' corresponding to the image
// currently being processed by the script
def entry = getProjectEntry()
// Get the name for the image & append a suitable file extension
// (here, .txt)
def name = entry.getImageName() + '.txt'
def path = buildFilePath(PROJECT_BASE_DIR, 'annotation locations')
mkdirs(path)
path = buildFilePath(path, name)
saveAnnotationMeasurements(path)
println 'Results exported to ' + path

def out = new File(path)


def name2 = entry.getImageName() + '_patches.txt'
def path2 = buildFilePath(PROJECT_BASE_DIR, 'patches')
mkdirs(path2)
path2 = buildFilePath(path2, name2)

def file = new File(path2)
    
out << x

class1 = getPathClass('Patch')

for (pathObject in getAnnotationObjects()) {
    if (pathObject.getPathClass() == class1) {
        def roi = pathObject.getROI()
        def co = roi.getAllPoints()
        file << co
}
}
