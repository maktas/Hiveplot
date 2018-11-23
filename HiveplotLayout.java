
package org.maktas.hiveplot;

import org.maktas.hiveplot.NodeComparator.CompareType;
import java.awt.geom.Point2D;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.gephi.data.attributes.api.AttributeColumn;
import org.gephi.data.attributes.api.AttributeController;
import org.gephi.data.attributes.api.AttributeModel;
import org.gephi.data.attributes.api.AttributeOrigin;
import org.gephi.data.attributes.api.AttributeTable;
import org.gephi.data.attributes.api.AttributeType;
import org.gephi.graph.api.Graph;
import org.gephi.graph.api.Node;
import org.gephi.layout.plugin.AbstractLayout;
import org.gephi.layout.plugin.ForceVectorNodeLayoutData;
import org.gephi.layout.spi.Layout;
import org.gephi.layout.spi.LayoutProperty;
import org.gephi.statistics.plugin.Degree;
import org.gephi.statistics.plugin.GraphDistance;
import org.openide.util.Exceptions;
import org.openide.util.Lookup;
import org.openide.util.NbBundle;

public class HiveplotLayout extends AbstractLayout implements Layout
{
    private float canvasArea;           // Set boundary for node placement.
    private int numAxes;                // Total number of axes.
    private String axesOrderProperty;   // Propert which dictates node axis.
    private String nodeOrderProperty;   // Property which dictates node order.
    protected Graph graph;              // The graph being laid out.
    public static final String BETWEENNESS = "betweenesscentrality";
    public static final String CLOSENESS = "closnesscentrality";
    public static final String ECCENTRICITY = "eccentricity";
    public static final String DEGREE = "degree";
    private int[] nodeAxis;             // The array for storing which axis the node belongs to
    private int[] axisNeighbors;        // The array for storing the number of the neighbor nodes  that are on the same axis

    // Debugging
    private final Logger LOGGER = Logger.getLogger(getClass().getName());


    /**
     * @param layoutBuilder 
     */
    public HiveplotLayout(HiveplotLayoutBuilder layoutBuilder)
    {
        super(layoutBuilder);
        this.canvasArea = 5000;
        this.numAxes = 3;
        this.axesOrderProperty = GraphDistance.BETWEENNESS;
        this.nodeOrderProperty = Degree.DEGREE;
        LOGGER.setLevel(Level.INFO);
    }
    
    @Override
    public void initAlgo()
    {
        this.graph = graphModel.getGraphVisible();
        for (Node n : graph.getNodes())
            n.getNodeData().setLayoutData(new ForceVectorNodeLayoutData());
        
        AttributeModel attributeModel = Lookup.getDefault().lookup(AttributeController.class).getModel();
        AttributeTable nodeTable = attributeModel.getNodeTable();
        AttributeColumn eccentricityCol = nodeTable.getColumn(ECCENTRICITY);
        AttributeColumn closenessCol = nodeTable.getColumn(CLOSENESS);
        AttributeColumn betweenessCol = nodeTable.getColumn(BETWEENNESS);
        AttributeColumn degreeCol = nodeTable.getColumn(DEGREE);
        
        if (nodeTable.getColumn(ECCENTRICITY) == null) {
            eccentricityCol = nodeTable.addColumn(ECCENTRICITY, "Eccentricity", AttributeType.DOUBLE, AttributeOrigin.COMPUTED, new Double(0));
        }
        if (nodeTable.getColumn(CLOSENESS) == null) {
            closenessCol = nodeTable.addColumn(CLOSENESS, "Closeness Centrality", AttributeType.DOUBLE, AttributeOrigin.COMPUTED, new Double(0));
        }
        if (nodeTable.getColumn(BETWEENNESS) == null) {
            betweenessCol = nodeTable.addColumn(BETWEENNESS, "Betweenness Centrality", AttributeType.DOUBLE, AttributeOrigin.COMPUTED, new Double(0));
        }
        if (nodeTable.getColumn(DEGREE) == null) {
            degreeCol = nodeTable.addColumn(DEGREE, "Degree", AttributeType.DOUBLE, AttributeOrigin.COMPUTED, new Double(0));
        }
        
        GraphDistance gd = new GraphDistance();
        gd.execute(graphModel, attributeModel);
        Degree deg = new Degree();
        deg.execute(graphModel, attributeModel);
    }
    
    @Override
    public void goAlgo() 
    {
        this.graph.readLock();
        nodeAxis = new int[this.graph.getNodeCount()];
        axisNeighbors = new int[this.graph.getNodeCount()];
        boolean axisSort = this.nodeOrderProperty != null && !this.nodeOrderProperty.equals("");
        double degree = 360/this.numAxes;                       // a' between axes
        float value;
        double dvalue;
        Double min, max;
        double dmin, dmax;

        List<Node[]> sortNodes = generateAxes(axisSort, false);  // Axes 
        Point2D.Float[] p = new Point2D.Float[this.numAxes];    // Max points for each axis
        Point2D.Float[] z = new Point2D.Float[this.numAxes];    // Next node position to draw
        Point2D.Float[] d = new Point2D.Float[this.numAxes];    // Avg. position delta
        
        // Calculate outer boundary points for all axes - centered on (0,0)
        for(int x = 0; x < this.numAxes; x++) {
            p[x] = new Point2D.Float(StrictMath.round(this.canvasArea * (Math.cos(Math.toRadians(degree * (x + 1))))),
                                     StrictMath.round(this.canvasArea * (Math.sin(Math.toRadians(degree * (x + 1))))));
            d[x] = new Point2D.Float(Math.abs(p[x].x), Math.abs(p[x].y));
        }   
        z = p;
        
        
        for(Node[] groups : sortNodes)
        {
            int index = 0;
            int pos = sortNodes.indexOf(groups);
                
            if(this.nodeOrderProperty.contentEquals("degree")){
                dmin = (double) this.graph.getDegree(groups[groups.length-1]);
                dmax = (double) this.graph.getDegree(groups[0]);
            }
            else{
                min = (Double) groups[groups.length-1].getNodeData().getAttributes().getValue(this.nodeOrderProperty);
                dmin = (double) min;
                max = (Double) groups[0].getNodeData().getAttributes().getValue(this.nodeOrderProperty);
                dmax = (double) max;
            }
            
            
            for (Node n : groups)
            {
                //Getting the value of each node for the node ordering property
                if(this.nodeOrderProperty.contentEquals("degree")){
                dvalue = (double) this.graph.getDegree(n);
                value = (float) dvalue;
                }
                else{
                dvalue = (Double) n.getNodeData().getAttributes().getValue(this.nodeOrderProperty);
                value = (float) dvalue;
                }
                
                if(index < groups.length){
                float ratio = (value-(float)dmin + 1) / ((float)dmax-(float)dmin + 1);
                
                z[pos] = new Point2D.Float((z[pos].x > 0 ? d[pos].x * ratio : -d[pos].x * ratio),
                                           (z[pos].y > 0 ? d[pos].y * ratio : -d[pos].y * ratio));
                }
                n.getNodeData().setX(z[pos].x);
                n.getNodeData().setY(z[pos].y);       
                
                index++;
            }

        } 

        this.graph.readUnlock();
    }


    /**
     * 
     */
    @Override
    public void endAlgo() 
    {
        for (Node n : graph.getNodes())
            n.getNodeData().setLayoutData(null);
    }
    
    @Override
    public boolean canAlgo() 
    {
        return true;
    }
    

    /**
     * @return 
     */
    @Override
    public LayoutProperty[] getProperties() 
    {
        List<LayoutProperty> properties = new ArrayList<LayoutProperty>();
        final String HIVEPLOT  = "Hiveplot Layout";

        try 
        {
            properties.add(LayoutProperty.createProperty(
                    this, Float.class,
                    NbBundle.getMessage(HiveplotLayout.class, "hiveplot.area.name"),
                    HIVEPLOT,
                    "hiveplot.area.name",
                    NbBundle.getMessage(HiveplotLayout.class, "hiveplot.area.desc"),
                    "getCanvasArea", "setCanvasArea"));
            properties.add(LayoutProperty.createProperty(
                    this, Integer.class,
                    NbBundle.getMessage(HiveplotLayout.class, "hiveplot.numAxes.name"),
                    HIVEPLOT,
                    "hiveplot.numAxes.name",
                    NbBundle.getMessage(HiveplotLayout.class, "hiveplot.numAxes.desc"),
                    "getNumAxes", "setNumAxes"));
            properties.add(LayoutProperty.createProperty(
                    this, String.class,
                    NbBundle.getMessage(HiveplotLayout.class, "hiveplot.axesOrderProperty.name"),
                    HIVEPLOT,
                    "hiveplot.axesOrderProperty.name",
                    NbBundle.getMessage(HiveplotLayout.class, "hiveplot.axesOrderProperty.desc"),
                    "getAxesOrderProperty", "setAxesOrderProperty"));
            properties.add(LayoutProperty.createProperty(
                    this, String.class,
                    NbBundle.getMessage(HiveplotLayout.class, "hiveplot.nodeOrderProperty.name"),
                    HIVEPLOT,
                    "hiveplot.nodeOrderProperty.name",
                    NbBundle.getMessage(HiveplotLayout.class, "hiveplot.nodeOrderProperty.desc"),
                    "getNodeOrderProperty", "setNodeOrderProperty"));
        } 
        catch (MissingResourceException e)
        {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            Exceptions.printStackTrace(e);
        }



        return properties.toArray(new LayoutProperty[0]);
    }


    /**
     * Resets the value of the layout attributes.
     */
    @Override
    public void resetPropertiesValues()
    {
        this.canvasArea = 5000;
        this.axesOrderProperty = GraphDistance.BETWEENNESS;
        this.nodeOrderProperty = Degree.DEGREE;
        this.numAxes = 3;
    }


    // Calculate axis boundaries
    
    /**
     * 
     * @return 
     */
    private List<Node[]> generateAxes(boolean sortNodesOnAxis, boolean asc)
    {
        ArrayList<Node[]> nodeGroups = new ArrayList<Node[]>();
        Node[] n = this.graph.getNodes().toArray();
        Arrays.sort(n, new NodeComparator(graph, n, CompareType.ATTRIBUTE, this.axesOrderProperty, true));

        int[] order = findBins(n);
        int lowerBound = 0;
        int upperBound = order[0];
        
        for(int bin = 0; bin < numAxes; bin++){
            nodeGroups.add((lowerBound != upperBound) ? Arrays.copyOfRange(n, lowerBound, upperBound) : Arrays.copyOf(n, lowerBound));
            if(bin < (order.length - 1))
            {
                lowerBound = upperBound;
                upperBound = lowerBound + order[bin + 1];
            }
        }
        
        // If the user selects sorting nodes along axes
        if(sortNodesOnAxis)
        {
            ArrayList<Node[]> nodeGroupsX = new ArrayList<Node[]>();
            for(Node[] nz : nodeGroups)
            {
                Arrays.sort(nz, new NodeComparator(graph, nz, CompareType.ATTRIBUTE, this.nodeOrderProperty, asc));
                nodeGroupsX.add(nz);
            }
            return nodeGroupsX;
        }
        else
        {
            return nodeGroups;
        }
    }
    
    /**
     * Generates an array of integers which represent bin cut-offs for sorted array.
     * @param nodes
     * @return 
     */
    private int[] findBins(Node[] nodes)
    {
        int totalBins = this.numAxes;
        double sum = 0.0;
        int[] bins = new int[totalBins];
        Double value;

        for(Node n : nodes)
        {
            if(this.axesOrderProperty.contentEquals("degree")){
                value = (double) this.graph.getDegree(n);
            }
            else{
            System.out.println(n.getNodeData().getId());
            value = (Double) n.getNodeData().getAttributes().getValue(this.axesOrderProperty);
            }
            sum += value;
        }

        double avg = sum / nodes.length;
        
        for (Node n : nodes)
        {
            if(this.axesOrderProperty.contentEquals("degree")){
                value = (double) this.graph.getDegree(n);
            }
            else{
            value = (Double) n.getNodeData().getAttributes().getValue(this.axesOrderProperty);
            }
            int binIndex = 0;
            for(int x=0; x < totalBins-1; x++)
            {
                if(value <= 2 * (x+1) * avg / totalBins){
                binIndex = x;
                break;
                }
                else binIndex = totalBins-1;
                
                nodeAxis[n.getId()] = binIndex;
                
            }
            bins[binIndex]++;
        }
        
        for(int x = 0; x < totalBins; x++){
            LOGGER.log(Level.INFO, "****** Bin{0} = {1}", new Object[]{x, bins[x]});
            //System.out.println("Total number of elements in axis" + (x+1) + "is: " + bins[x]);
        }

        return(bins);
    }


    // Accessors 


    /**
     * 
     * @return 
     */
    public int getNumAxes()
    {
     return this.numAxes;   
    }
    
    /**
     * 
     * @param numAxes 
     */
    public void setNumAxes(Integer numAxes)
    {
        this.numAxes = numAxes;
    }


    /**
     * 
     * @param axesOrderProperty
     * @return 
     */
    public String getAxesOrderProperty()
    {
        return this.axesOrderProperty;
    }
    
    /**
     * 
     * @param axesOrderProperty 
     */
    public void setAxesOrderProperty(String axesOrderProperty)
    {
        if(axesOrderProperty.toLowerCase().contentEquals("betweenness"))
        this.axesOrderProperty = BETWEENNESS;
        else if(axesOrderProperty.toLowerCase().contentEquals("closeness"))
        this.axesOrderProperty = CLOSENESS;
        else if(axesOrderProperty.toLowerCase().contentEquals("eccentricty"))
        this.axesOrderProperty = ECCENTRICITY;
        else if(axesOrderProperty.toLowerCase().contentEquals("degree"))
        this.axesOrderProperty = DEGREE;
        else this.axesOrderProperty = BETWEENNESS;
    }
    
    /**
     * 
     * @return 
     */
    public String getNodeOrderProperty()
    {
        return this.nodeOrderProperty;
    }
    
    /**
     * 
     * @param nodeOrderProperty 
     */
    public void setNodeOrderProperty(String nodeOrderProperty)
    {
        if(nodeOrderProperty.toLowerCase().contentEquals("betweenness"))
        this.nodeOrderProperty = BETWEENNESS;
        else if(nodeOrderProperty.toLowerCase().contentEquals("closeness"))
        this.nodeOrderProperty = CLOSENESS;
        else if(nodeOrderProperty.toLowerCase().contentEquals("eccentricty"))
        this.nodeOrderProperty = ECCENTRICITY;
        else if(nodeOrderProperty.toLowerCase().contentEquals("degree"))
        this.nodeOrderProperty = DEGREE;
        else this.nodeOrderProperty = DEGREE;
            
    }
    
    /**
     * 
     * @return 
     */
    public float getCanvasArea()
    {
        return this.canvasArea;
    }
    
    /**
     * 
     * @param area 
     */
    public void setCanvasArea(Float canvasArea)
    {
        this.canvasArea = canvasArea;
    }
}