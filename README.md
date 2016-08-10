# KymographTracker

Icy Kymograph tracker, by Nicolas Chenouard.

Check the Icy website for up to date documentation of the version distributed in Icy.
http://icy.bioimageanalysis.org/plugin/KymographTracker

This plugin allows the extraction of the kymograph representation of pixel intensity evolution through time and along a defined spatial path. Different utilities are embedded to define the extraction curve.

Multiple paths can be used for the same image sequence.

Once the kymograph representation is extracted, tracks can be traced as 2D curves in the kymograph images. Those tracks can be save as a sequence of 1D positions along the extraction path through time, or exported to the TrackManager pluging for 2D+T representation and analysis.

Results export and import functionalities are provided.

NB: only 2D+t image sequences can be proecessed and only the first channel is used for the extraction of the kymograph images.

For a reference, please cite:
Chenouard, N., Buisson, J., Bloch, I., Bastin, P. and Olivo-Marin, J.-C.
"Curvelet analysis of kymograph for tracking bi-directional particles in fluorescence microscopy images", 17th IEEE International Conference on Image Processing (ICIP), 2010


#Documentation

##Background

The Kymograph Tracker plug-in allows one to extract the multiple kymographs in image sequences and trace tracks in those images. A kymograph, or kymogram, is a 2D representation of spatio-temporal information: the pixel intensity evolution through time along a predefined spatial path in the original 2D images. The x- axis (horizontal) of the kymograph corresponds to the position along the path, while the y-axis (vertical) corresponds to the time frame in the image sequence. Hence, the pixel value at position (x, y) in the kymograph image corresponds to the pixel intensity at position x along the extraction path at frame y of the sequence. The kymograph is thus a concise and powerful 2D representation of a 2D+T information. The Kymograph Tracker plug-in allows one to trace multiple extraction paths and extract corresponding kymograph representations.

Objects moving along the extraction path in the original 2D+T image sequence typically create a trail in the kymograph representation. A static object will leave a perfectly vertical trace, while the slope of the trail is in direct correspondence with its velocity (the faster, the more horizontal). Thus, one can extract spatio-temporal trajectories (along the extraction path) of objects by tracing their traces in the kymograph representation. The kymograph Tracker offers some utilities to semi-automatically trace this kind of trajectories.

##How to use the Kymograph Tracker plugin?

###Analyis pipeline

There are currently 4 main sequential steps in the analysis pipeline brought by the Kymograph Tracker plug-in:

1/ Identification of extraction paths in a 2D+T image sequence,

2/ extraction of the kymograph representations corresponding to those paths,

3/ identification of object trajectories in kymograph images,

4/ analysis, export and conversion to a standard 2D + time representation of those trajectories.

Only steps 1/ and 3/ require human intervention while other processes are fully automated. Yet, the plug-in ships some utilities such that the user interventions are as few as possible by guiding the human work.

###Identification of extraction paths in a 2D+T image sequence

####Image sequence selection

The first step in the analysis the analysis pipeline is to select the image sequence from which the kymograph tracking procedure has to be applied to. To do so, go to the “Image sequence selection” tab and specify the image sequence to analyze in the dedicated box. Please, make sure that the sequence has multiple frames, and be advised that for multi-plane (stack) or multi-color images only the first plane and the first channel will be processed. Click then the button “Start kymograph tracking workflow” or go to the tab “Workflow/Kymograph extraction”. 

####Workflow - Kymograph extraction

In the “Workflow/Kymograph extraction” you will be asked to create (if necessary) and choose the paths used for building one or multiple kymograph representation. In general, the kymograph representation is extracted from ROIs present in the processed image sequence. If none is present, multiple choices are given to the user:

1/ directly trace ROIs in the image sequence with ICY standard ROI creation tools (button “use ROIs in the original sequence”),

2/ semi-automatically trace a path in the image sequence that will be automatically converted to a valid ROI (“trace a path in the original image sequence”),

3/ semi automatically trace a path in a feature-enhanced representation of the image sequence (“trace a path in an enhanced projection of the sequence”).

####Path tracing

In case 2/ and 3/, we refer to a ‘path’ as a spatial curve that can be traced semi-automatically by clicking control points in the image sequence or its feature-enhanced projection. The path is automatically built by linking control points such that the curve follows a high pixel-value trail. In practice, the user clicks as many control points as required and the path is terminated by pressing the ESC key. Note here the blue cross marker under the mouse pointer which indicates that the point clicking ability is enabled. A yellow curve denotes a temporary path not yet validated by a user click action, while a fixed path is traced in red. After the path is validated (by pressing the ESC key), a corresponding ROI is automatically created in the image sequence from which to create the kymograph representations.

The plug-in is able to process multiple extraction paths at once for kymograph representation. To do so, create multiple ROI in the image sequence or use the path creation tools described above multiple times.

The Kymograph Tracker plug-in offers multiple options for kymograph representation from a ROI:

-          The value ‘Radius of averaging area’ corresponds to the radius of the disk used for averaging values of the original image sequence around the sampling points defined by the ROIs.

-          When enabled, the ‘Split anterograde and retrograde’ option automatically yields two kymograph images for a single extraction ROI. One will correspond to anterograde-moving objects, while the second one corresponds to retrograde ones. This can prove useful in case of dense bi-directional trafficking along the extraction path.

###Construction of the kymograph representations corresponding to extraction paths

One extraction ROI have been created and options for extraction have been set, press the ‘Extract kymographs’ button to proceed with the creation of the kymograph representations. New images corresponding to the created kymographs will then appear in the main working area of ICY.

Press then the button ‘Trace tracks in kymographs’ or the tab ‘Workflow/Kymograph analysis and tracking’ to proceed with the step 3/ of the main analysis pipeline: trajectory identification in kymographs.

###Identification of object trajectories in kymograph images

In kymograph images, trajectories of bright objects usually appear as almost continuous trails. Spatio-temporal tracks can thus be identified by tracing those trails. To do so, select first the kymograph to process in the table of current kymograph representations in the “Track creation” tab, and then proceed with the standard procedure for track identification described below.

The general procedure for track identification is to trace ROIs in the kymograph images and then convert them in a track format using the “Convert ROIs to tracks” button. Once again, one can use semi-automatic tools for easy track identification instead of the standard ROI creation tools of ICY. To do so, press the ‘Trace tracks as path’ button and trace path in the kymograph sequence in the very same way paths can be traced at step 1/ for kymograph extraction (see Path tracing section): click control points that are automatically linked and press the ESC key to validate the path. A corresponding ROI is then automatically created.

Once ROIs corresponding to trajectories have been built in the kymograph image, press the button “Convert ROIs to tracks” and click the “Track visualization” tab for examining results.

###Analysis, export and conversion to a standard 2D + time representation of those trajectories.

The ‘Track visualization’ tab gives a summary of the tracks that have been extracted from the current kymograph images. From there one can convert back trajectories to a standard 2D+T format by pressing the ‘Show 2D tracks’ button. Tracks are then converted and exported to the Track Manager plug-in which is automatically launched for the source sequence. 2D trajectories can be analyzed and saved with the different utilities offered by this plug-in.

A second way of exporting and saving results is through the ‘Workflow/Results output’ tab. In this tab, a table summarizes the analyzed kymographs and trajectories and let the user choose some to be exported.  Press the button ‘Export results’ to save kymographs and trajectories at a location. Results can then be loaded back in the ‘Import previous work’ tab by selecting the XML file which contains main results. Kymographs and tracks should  be available for further processing after being imported.
