// edited from https://github.com/saramcardle/Image-Analysis-Scripts/blob/master/QuPath%20Groovy%20Scripts/RareCellFetcher.groovy

resolveHierarchy()
getAnnotationObjects().each{
    if(it.getParent().getPathClass() == getPathClass("Partial")){
    removeObject(it, true)
    }
}
topLevel = getCurrentHierarchy().getRootObject().getChildObjects()
def newPathClass = getPathClass("Not Checked")
topLevel.forEach {
   it.setPathClass(newPathClass)
}
//To look into - not always keeping the same order
def classifications = [getPathClass("Patch"),getPathClass("Bad"),getPathClass("Clone"), getPathClass("Not Checked"), getPathClass("Partial")];//new ArrayList<>(cells.collect {it.getPathClass()} as Set)

/*
Minor change to getNextCell so that it searches classified annotations that don't have the same class as current cells.
Function to help you annotate saingle, rare cells.
Run this after detecting cells and beginning to train an object classifier.
Shows you random cells that it thinks are in your desired class. You can assign them to a class,
and it automatically moves to the next cell. Update the classifier regularly to see your improved results.

Inspired by "fetch" command in Cell Profiler Analyst.

Written by Sara McArdle of the La Jolla Institute and edited by Michael Nelson, 2020.
Updated for QuPath 0.3.0 July 2021
*/


//Made a list to hold tracking information from cells that are analyzed.
//No tracking data is collected by the developers
pastCells = []
name = getProjectEntry().getImageName()+".training"
dirpath = buildFilePath(PROJECT_BASE_DIR, 'TrainingAnnotations')
Logger logger = LoggerFactory.getLogger(QuPathGUI.class);
mkdirs(dirpath)
path = buildFilePath(PROJECT_BASE_DIR, 'TrainingAnnotations', name)
//function to get a random cell of a desired class (interest) that isn't already annotated
//chooses a cell, copies it's ROI to an annotation
def getNextCell(interest, classifications, pastCells){
   cells= getQuPath().getImageData().getHierarchy().getAnnotationObjects()
   potentials =cells.findAll{it.getPathClass()==getPathClass(interest)}
   if (potentials.size() == 0) {
       print("Must have objects of the correct class")
       logger.info("Must have objects of the correct class")
       Dialogs.showErrorMessage("Selection", "You must have objects of the selected class first!")
       return
   }
   Random rnd = new Random()
   position = rnd.nextInt(potentials.size())
   logger.info("Fly, you fools! {}", position)
   selection=potentials[position]

   def roi=selection.getROI()
   tempAnnot = PathObjects.createAnnotationObject(roi)
   logger.info("Flew!")
   getQuPath().getImageData().getHierarchy().selectionModel.setSelectedObject(selection,false)
   logger.info("after")
   //add the current cell to a list of cells so that the user can backtrack if something was incorrectly skipped or assigned
   pastCells << selection
}

//get existing classes
cells= getAnnotationObjects()
if (cells.size()==0) {
   print("Must have detection objects")
}
if (classifications.size()<2){
   print("Must have cells assigned to at least 2 classes")
}

def classStr=classifications.collect{it.toString()}
def classObs= FXCollections.observableArrayList(classStr)

//list view for assigning classes (and label)
/*ListView<String> classListView = new ListView<String>(classObs)
if (classStr.size()<6) {
   classListView.setPrefHeight((classStr.size() * 24) + 4)
} else {
   classListView.setPrefHeight((6 * 24) + 4)
}
Label assignmentLabel = new Label("Assign to which class:")
*/
//drop down for choosing which class to fetch (and label)
ChoiceBox classChoiceBox = new ChoiceBox(classObs)
classChoiceBox.setMaxWidth(Double.MAX_VALUE)
Label fetchLabel = new Label("Pick a label: \n\n\n")

Hyperlink helpLink = new Hyperlink("Help guide")
helpLink.setOnAction{e->
   if (Desktop.isDesktopSupported()){
       Desktop.getDesktop().browse(new URI("https://forum.image.sc/t/rarecellfetcher-a-tool-for-annotating-rare-cells-in-qupath/33654"))
   }
}

/*//Export and remove annotations. Needed to move this up in the code so that the assign button could trigger it going active
Button saveButton = new Button("Save and Remove Annotations")
saveButton.setOnAction {e ->

   //Need to add an alert of some sort here, since right now if you double click you will overwrite
   //with an empty file and delete everything.
   Alert alert = new Alert(AlertType.CONFIRMATION, "All classified annotations will be saved\nand then deleted. Proceed?", ButtonType.YES, ButtonType.NO, ButtonType.CANCEL);
   alert.showAndWait();

   if (alert.getResult() == ButtonType.YES) {


       toRemove = getQuPath().getImageData().getHierarchy().getAnnotationObjects().findAll{classifications.contains(it.getPathClass())}
       toExport = toRemove.collect {new qupath.lib.objects.PathAnnotationObject(it.getROI(), it.getPathClass())}
       //name = getQuPath().getProject().getProjectEntry().getImageName()+".training"
       //dirpath = buildFilePath(PROJECT_BASE_DIR, 'TrainingAnnotations')
       //mkdirs(dirpath)
       //path = buildFilePath(PROJECT_BASE_DIR, 'TrainingAnnotations', name)


       new File(path).withObjectOutputStream {
           it.writeObject(toExport)
       }

       getQuPath().getImageData().getHierarchy().removeObjects(toRemove,true)
       getQuPath().getImageData().getHierarchy().resolveHierarchy()
   }

}
saveButton.setMaxWidth(Double.MAX_VALUE)
saveButton.setDisable(true) //start disabled until a class is fetched the first time


//Restore removed annotations
Button reloadButton = new Button("Reload current image annotations")
reloadButton.setOnAction {e ->

   Alert alert = new Alert(AlertType.CONFIRMATION, "All annotations from the save file will be placed \nback into the image! Proceed?", ButtonType.YES, ButtonType.NO, ButtonType.CANCEL);
   alert.showAndWait();

   if (alert.getResult() == ButtonType.YES) {

       //name = getProjectEntry().getImageName()+".training"
       //dirpath = buildFilePath(PROJECT_BASE_DIR, 'TrainingAnnotations')
       //path = buildFilePath(PROJECT_BASE_DIR, 'TrainingAnnotations', name)
       //I was not able to find a cleaner way to do this without throwing an error if the file did not exist.
       File file = new File(path)
       if (file.exists()){
           file.withObjectInputStream {
           toReload = it.readObject()
           getQuPath().getImageData().getHierarchy().addPathObjects(toReload)
           resolveHierarchy()
       }


       }
   }
}
reloadButton.setMaxWidth(Double.MAX_VALUE)
*/


//Set up a button to assign a class
Button assignButton = new Button("Assign Clone (C)")
assignButton.setOnAction {e ->
//print classListView.selectionModel.selectedItem

   getQuPath().getImageData().getHierarchy().selectionModel.setSelectedObject(selection,false)
   selection.setPathClass(getPathClass("Clone"));
   //getQuPath().getImageData().getHierarchy().updateObject(selection, true)
   //getQuPath().getImageData().getHierarchy().addPathObject(tempAnnot)
   //getQuPath().getImageData().getHierarchy().insertPathObject(tempAnnot, true)

   getNextCell(classChoiceBox.selectionModel.getSelectedItem().toString(), classifications, pastCells)
   //saveButton.setDisable(false)

   getQuPath().getImageData().getHierarchy().selectionModel.setSelectedObject(selection,true)
   QuPathGUI.getInstance().getViewer().setCenterPixelLocation(selection.getROI().getCentroidX(),selection.getROI().getCentroidY())
}
assignButton.setMaxWidth(Double.MAX_VALUE)
assignButton.setDisable(true) //start disabled until a class is fetched the first time

//Second fixed class assignment button
Button assignButton2 = new Button("Assign Patch (P)")
assignButton2.setOnAction {e ->

   selection.setPathClass(getPathClass("Patch"));
   //getQuPath().getImageData().getHierarchy().addPathObject(tempAnnot)
   //getQuPath().getImageData().getHierarchy().insertPathObject(tempAnnot, true)
   getNextCell(classChoiceBox.selectionModel.getSelectedItem().toString(), classifications, pastCells)
   //saveButton.setDisable(false)

   getQuPath().getImageData().getHierarchy().selectionModel.setSelectedObject(selection,true)
   QuPathGUI.getInstance().getViewer().setCenterPixelLocation(selection.getROI().getCentroidX(),selection.getROI().getCentroidY())
}
assignButton2.setMaxWidth(Double.MAX_VALUE)
assignButton2.setDisable(true) //start disabled until a class is fetched the first time

//Third fixed class assignment button
Button assignButton3 = new Button("Assign Partial (R)")
assignButton3.setOnAction {e ->

   selection.setPathClass(getPathClass("Partial"));
   //getQuPath().getImageData().getHierarchy().addPathObject(tempAnnot)
   //getQuPath().getImageData().getHierarchy().insertPathObject(tempAnnot, true)
   getNextCell(classChoiceBox.selectionModel.getSelectedItem().toString(), classifications, pastCells)
   //saveButton.setDisable(false)

   getQuPath().getImageData().getHierarchy().selectionModel.setSelectedObject(selection,true)
   QuPathGUI.getInstance().getViewer().setCenterPixelLocation(selection.getROI().getCentroidX(),selection.getROI().getCentroidY())
}
assignButton3.setMaxWidth(Double.MAX_VALUE)
assignButton3.setDisable(true) //start disabled until a class is fetched the first time

//Fourth fixed class assignment button
Button assignButton4 = new Button("Assign Bad (F)")
assignButton4.setOnAction {e ->

   selection.setPathClass(getPathClass("Bad"));
   //getQuPath().getImageData().getHierarchy().addPathObject(tempAnnot)
   //getQuPath().getImageData().getHierarchy().insertPathObject(tempAnnot, true)
   getNextCell(classChoiceBox.selectionModel.getSelectedItem().toString(), classifications, pastCells)
   //saveButton.setDisable(false)

   getQuPath().getImageData().getHierarchy().selectionModel.setSelectedObject(selection,true)
   QuPathGUI.getInstance().getViewer().setCenterPixelLocation(selection.getROI().getCentroidX(),selection.getROI().getCentroidY())
}
assignButton4.setMaxWidth(Double.MAX_VALUE)
assignButton4.setDisable(true) //start disabled until a class is fetched the first time


//skip button for if you do not like a cell and do not want to annotate it
Button skipButton = new Button("Skip (S)")
skipButton.setOnAction {e ->
   getNextCell(classChoiceBox.selectionModel.getSelectedItem().toString(), classifications, pastCells)
   getQuPath().getImageData().getHierarchy().selectionModel.setSelectedObject(selection,true)
   QuPathGUI.getInstance().getViewer().setCenterPixelLocation(selection.getROI().getCentroidX(),selection.getROI().getCentroidY())
}
skipButton.setMaxWidth(Double.MAX_VALUE)
skipButton.setDisable(true) //start disabled until a class is fetched the first time

//back up button for if user made a mistake with a cell
Button backButton = new Button("Back (B)")
backButton.setOnAction {e ->
   if(pastCells.size() > 1){
       selection=pastCells[pastCells.size()-2]
       getQuPath().getImageData().getHierarchy().selectionModel.setSelectedObject(selection,false)
       pastCells.remove(pastCells[pastCells.size()-1])
       getQuPath().getImageData().getHierarchy().selectionModel.setSelectedObject(selection,true)
       QuPathGUI.getInstance().getViewer().setCenterPixelLocation(selection.getROI().getCentroidX(),selection.getROI().getCentroidY())
   }
}
backButton.setMaxWidth(Double.MAX_VALUE)
backButton.setDisable(true) //start disabled until a class is fetched the first time

//highlight button in case you "un-select" a cell and forget which one is being shown
Button highlightButton = new Button("Highlight Line (H)")
highlightButton.setOnAction {e ->
   getQuPath().getImageData().getHierarchy().selectionModel.setSelectedObject(selection,true)
   QuPathGUI.getInstance().getViewer().setCenterPixelLocation(selection.getROI().getCentroidX(),selection.getROI().getCentroidY())
}
highlightButton.setMaxWidth(Double.MAX_VALUE)
highlightButton.setDisable(true)

//when a class is chosen, fetch a cell and enable all the rest of the buttons
classChoiceBox.getSelectionModel().selectedItemProperty().addListener({v,o,n->
logger.info(" {} test",n.toString())
//logger.info(classifications)
logger.info(" List of cells: {}", pastCells)
   getNextCell(n.toString(), classifications, pastCells)
   assignButton.setDisable(false)
   assignButton2.setDisable(false)
   assignButton3.setDisable(false)
   assignButton4.setDisable(false)
   skipButton.setDisable(false)
   highlightButton.setDisable(false)
   backButton.setDisable(false)

} as ChangeListener)

//put all buttons into a grid pane
GridPane gridPane = new GridPane();
gridPane.setMinSize(100, 120);
gridPane.setPadding(new Insets(10, 10, 10, 10));
gridPane.setVgap(5);
gridPane.setHgap(10);
gridPane.setAlignment(Pos.CENTER);

//gridPane.add is read (object,Column,Row) and is 0-based
gridPane.add(fetchLabel,0,0)
gridPane.setHalignment(fetchLabel, HPos.RIGHT)
gridPane.add(classChoiceBox,1,0)
//gridPane.add(assignmentLabel,0,1)
gridPane.add(helpLink,1,1)
gridPane.setHalignment(helpLink, HPos.RIGHT)
//gridPane.setHalignment(assignmentLabel, HPos.CENTER)
//gridPane.add(classListView,0,2,1,4)
gridPane.add(assignButton,1,2)
gridPane.add(assignButton2,1,3)
gridPane.add(assignButton3,1,4)
gridPane.add(assignButton4,1,5)
gridPane.add(skipButton,1,6)
gridPane.add(backButton,1,7)
gridPane.add(highlightButton,1,8)
//gridPane.add(saveButton,0,6,2,1)
//gridPane.add(reloadButton,0,8,2,1)

gridPane.setOnKeyPressed{event ->
   KeyCode key = event.getCode()
   if(key.equals(KeyCode.S)){
       skipButton.fire();
   }
   if(key.equals(KeyCode.C)){
       assignButton.fire();
   }
   if(key.equals(KeyCode.P)){
       assignButton2.fire();
   }
   if(key.equals(KeyCode.R)){
       assignButton3.fire();
   }
   if(key.equals(KeyCode.F)){
       assignButton4.fire();
   }       
   if(key.equals(KeyCode.H)){
       highlightButton.fire();
   }
   if(key.equals(KeyCode.B)){
       backButton.fire();
   }
}
//show the GUI
Platform.runLater { //something about threading I do not understand. Copied from Pete.
   def stage = new Stage()
   stage.initOwner(QuPathGUI.getInstance().getStage())
   stage.setScene(new Scene(gridPane))
   stage.setTitle("Let's go on a clone hunt")
   stage.show()
}
import javafx.application.Platform
import qupath.lib.gui.QuPathGUI
import qupath.lib.gui.scripting.QPEx
import javafx.scene.control.ListView
import javafx.collections.FXCollections
import javafx.scene.layout.GridPane;
import javafx.scene.control.ChoiceBox
import javafx.geometry.Insets
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.stage.Stage
import javafx.scene.control.Label
import javafx.scene.control.Button
import javafx.beans.value.ChangeListener
import javafx.geometry.HPos
import javafx.scene.input.KeyCode
import javafx.scene.control.Hyperlink
import javafx.scene.control.Alert
import javafx.scene.control.Alert.AlertType
import javafx.scene.control.ButtonType
import java.awt.Desktop
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
