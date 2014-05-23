package org.eclipse.bpmn2.modeler.ui.diagram;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;

import javax.imageio.ImageIO;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.SWTGraphics;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.gef.EditPart;
import org.eclipse.gef.GraphicalEditPart;
import org.eclipse.gef.GraphicalViewer;
import org.eclipse.gef.LayerConstants;
import org.eclipse.gef.editparts.LayerManager;
import org.eclipse.graphiti.features.IFeatureProvider;
import org.eclipse.graphiti.features.context.ISaveImageContext;
import org.eclipse.graphiti.internal.util.T;
import org.eclipse.graphiti.ui.features.DefaultSaveImageFeature;
import org.eclipse.graphiti.ui.internal.services.GraphitiUiInternal;
import org.eclipse.graphiti.ui.saveasimage.ISaveAsImageConfiguration;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.ImageLoader;
import org.eclipse.swt.graphics.PaletteData;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

public class FoxBPMSaveImageFeature extends DefaultSaveImageFeature {
        private IFigure _allFigure;
        private Image _imageAll;
        private String filename;
        private String languageType;
        private String processKey;
        private String dbIdString;
        private byte image[];

        public FoxBPMSaveImageFeature(IFeatureProvider fp) {
                super(fp);
        }

        @Override
        public void save(ISaveImageContext context) {
                // Get viewer containing the diagram to print (by default the one
                // contained in the diagram editor that starts this feature
                GraphicalViewer viewer = getGraphicalViewer(context);

                // Select filename with file-dialog
//                String filePath = FixFlowConfigUtil.getFixflowDiagramPath() + languageType + "/" + processKey + "/";
              String filePath = "";
                File file = new File(filePath);
                        if(!file.exists()) {
                                file.mkdirs();
                        }
                filename = filePath + dbIdString + ".PNG";
                
                // Configure and open dialog
                ISaveAsImageConfiguration saveAsImageConfiguration = getSaveAsImageConfiguration(viewer);

                EditPart rootEditPart = viewer.getRootEditPart();
                if (!(rootEditPart instanceof GraphicalEditPart))
                        return;

                // determine _allFigure
                GraphicalEditPart graphicalRootEditPart = (GraphicalEditPart) rootEditPart;
                IFigure rootFigure = ((LayerManager) graphicalRootEditPart).getLayer(LayerConstants.PRINTABLE_LAYERS);
                if (rootFigure == null)
                        return;

                _allFigure = rootFigure;
                
                initImageAll(10000.0d, viewer);
                
                if (filename != null) {
                        Shell shell = GraphitiUiInternal.getWorkbenchService().getShell();
                        try {
                                // Add extension to filename (if none exists)
                                filename = addFileExtension(saveAsImageConfiguration.getFormattedFileExtension(), filename);

                                // Create the save as image operation ...
                                IRunnableWithProgress operation = getSaveAsImageOperation(saveAsImageConfiguration, filename);

                                // ... and start save as image
                                new ProgressMonitorDialog(shell).run(false, false, operation);
                        } catch (InterruptedException e) {
                                T.racer().warning("Save as image operation was cancelled by user"); //$NON-NLS-1$
                        } catch (Exception e) {
                                String message = "Cannot save image: "; //$NON-NLS-1$
                                MessageDialog.openError(shell, "Cannot save image", message + e.getMessage()); //$NON-NLS-1$
                                T.racer().error(message, e);
                        }
                }
        }
        
        private void initImageAll(double upperBoundPixels, GraphicalViewer graphicalViewer) {
                {
                        int width = _allFigure.getBounds().width;
                        int height = _allFigure.getBounds().height;

                        // check whether the dimensions of the image to be created would
                        // be small enough to prevent runtime exceptions
                        if (width <= upperBoundPixels && height <= upperBoundPixels) {
                                _imageAll = new Image(Display.getDefault(), width, height);
                                GC gc = new GC(_imageAll);
                                SWTGraphics graphics = new SWTGraphics(gc);

                                /* move all figures into the positive region */
                                EditPart contents = graphicalViewer.getContents();
                                if (contents instanceof GraphicalEditPart) {
                                        IFigure contentsFigure = ((GraphicalEditPart) contents).getFigure();
                                        Rectangle contentBounds = contentsFigure.getBounds();
                                        graphics.translate(-contentBounds.x, -contentBounds.y);
                                }

                                _allFigure.paint(graphics);

                                if (gc != null)
                                        gc.dispose();
                                if (graphics != null)
                                        graphics.dispose();
                        } else {
                                _imageAll = null;
                        }
                }
        }

        @Override
        protected IRunnableWithProgress getSaveAsImageOperationForStandardExporter(ISaveAsImageConfiguration saveAsImageConfiguration, final String filename) {
                int imageFormat = SWT.IMAGE_PNG;
                final byte imageBytes[] = convertImageToBytes(_imageAll, imageFormat);
                image = imageBytes;
                IRunnableWithProgress operation = new IRunnableWithProgress() {
                        public void run(IProgressMonitor monitor) throws InvocationTargetException {
                                FileOutputStream outputStream = null;
                                try {
                                        System.out.println(filename);
                                        outputStream = new FileOutputStream(filename);
                                        outputStream.write(imageBytes);
                                } catch (Exception e) {
                                        throw new InvocationTargetException(e);
                                } finally {
                                        try {
                                                outputStream.close();
                                        } catch (Exception x) {
                                                T.racer().error("close output stream failed", x); //$NON-NLS-1$
                                        }
                                }
                                
                                 File f = new File(filename);   
                     f.canRead();   
                     f.canWrite();   
                     BufferedImage src;
                                try {
                                        src = ImageIO.read(f);
                                        ImageIO.write(src, "PNG", new File(filename));
                                } catch (IOException e) {
                                        e.printStackTrace();
                                }   
                        }
                };
                return operation;
        }

        private byte[] convertImageToBytes(Image image, int format) {
                ByteArrayOutputStream result = new ByteArrayOutputStream();

                try {
                        ImageData imDat = null;
                        // Save as GIF is only working if not more than 256 colors are used
                        // in the image
                        if (format == SWT.IMAGE_GIF) {
                                imDat = create8BitIndexedPaletteImage(image);
                        }

                        if (imDat == null) {
                                imDat = image.getImageData();
                        }

                        ImageLoader imageLoader = new ImageLoader();
                        imageLoader.data = new ImageData[] { imDat };
                        try {
                                imageLoader.save(result, format);
                        } catch (SWTException e) {
                                String error = "Depth: " + Integer.toString(image.getImageData().depth) + "\n" + "X: " //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                                                + Integer.toString(image.getImageData().x)
                                                + "\n" + "Y: " + Integer.toString(image.getImageData().y); //$NON-NLS-1$ //$NON-NLS-2$
                                throw new IllegalStateException(error, e);
                        }
                } catch (Exception e) {
                        throw new IllegalStateException(e);
                } finally {
                        image.dispose();
                }

                return result.toByteArray();
        }
        
        private ImageData create8BitIndexedPaletteImage(Image image) {
                int upperboundWidth = image.getBounds().width;
                int upperboundHeight = image.getBounds().height;
                ImageData imageData = image.getImageData();

                // determine number of used colors
                ArrayList<Integer> colors = new ArrayList<Integer>();
                for (int x = 0; x < upperboundWidth; x++) {
                        for (int y = 0; y < upperboundHeight; y++) {
                                int color = imageData.getPixel(x, y);
                                Integer colorInteger = new Integer(color);
                                if (!colors.contains(colorInteger))
                                        colors.add(colorInteger);
                        }
                }

                // at the moment this is only working if not more than 256 colors are
                // used in the image
                if (colors.size() > 256) {
                        throw new IllegalStateException(
                                        "Image contains more than 256 colors. \n Automated color reduction is currently not supported."); //$NON-NLS-1$
                }

                // create an indexed palette
                RGB[] rgbs = new RGB[256];
                for (int i = 0; i < 256; i++)
                        rgbs[i] = new RGB(255, 255, 255);
                for (int i = 0; i < colors.size(); i++) {
                        int pixelValue = ((colors.get(i))).intValue();
                        int red = (pixelValue & imageData.palette.redMask) >>> Math.abs(imageData.palette.redShift);
                        int green = (pixelValue & imageData.palette.greenMask) >>> Math.abs(imageData.palette.greenShift);
                        int blue = (pixelValue & imageData.palette.blueMask) >>> Math.abs(imageData.palette.blueShift);
                        rgbs[i] = new RGB(red, green, blue);
                }

                // create new imageData
                PaletteData palette = new PaletteData(rgbs);
                ImageData newImageData = new ImageData(imageData.width, imageData.height, 8, palette);

                // adjust imageData with regard to the palette
                for (int x = 0; x < upperboundWidth; x++) {
                        for (int y = 0; y < upperboundHeight; y++) {
                                int color = imageData.getPixel(x, y);
                                newImageData.setPixel(x, y, colors.indexOf(new Integer(color)));
                        }
                }

                return newImageData;
        }

        @Override
        protected IRunnableWithProgress getSaveAsImageOperation(ISaveAsImageConfiguration saveAsImageConfiguration, String filename) {
                IRunnableWithProgress operation = null;

                String imageExtension = saveAsImageConfiguration.getFileExtension();
                if (getDiagramExporters().containsKey(imageExtension)) {
                        // If the exporter is non-standard, i.e. registered via
                        // extension point, we need to call the registered
                        // exporter
                        operation = getSaveAsImageOperationForNonStandardExporter(saveAsImageConfiguration, filename);
                } else {
                        // Handle internal image format
                        operation = getSaveAsImageOperationForStandardExporter(saveAsImageConfiguration, filename);
                }
                return operation;
        }

        public void setLanguageType(String languageType) {
                this.languageType = languageType;
        }

        public void setProcessKey(String processKey) {
                this.processKey = processKey;
        }

        public void setDbIdString(String dbIdString) {
                this.dbIdString = dbIdString;
        }

		public byte[] getImage() {
			return image;
		}
}