package com.roboclub.robobuggy.main;

import java.awt.Color;
import java.util.Date;

import javax.swing.BoxLayout;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import com.roboclub.robobuggy.logging.RobotLogger;
import com.roboclub.robobuggy.serial.SerialEvent;
import com.roboclub.robobuggy.serial.SerialListener;

public class ImuPanel extends SerialPanel {
	private static final long serialVersionUID = -929040896215455343L;
	
	/* Constants for serial communication */
	/** Header char array for choosing serial port */
	private static final char[] HEADER = {'#', 'A', 'C', 'G','='};
	/** Length of header char array */
	private static final int HEADER_LEN = 5;
	/** Baud rate for serial port */
	private static final int BAUDRATE = 57600;
	/** Index of accel x data as received during serial communication */
	private static final int AX = 0;
	/** Index of accel y data as received during serial communication */
	private static final int AY = 1;
	/** Index of accel z data as received during serial communication */
	private static final int AZ = 2;
	/** Index of gyro x data as received during serial communication */
	private static final int RX = 3;
	/** Index of gyro y data as received during serial communication */
	private static final int RY = 4;
	/** Index of gyro z data as received during serial communication */
	private static final int RZ = 5;
	/** Index of magnetometer x data as received during serial communication */
	private static final int MX = 6;
	/** Index of magnetometer y data as received during serial communication */
	private static final int MY = 7;
	/** Index of magnetometer z data as received during serial communication */
	private static final int MZ = 8;
	
	private float aX;
	private float aY;
	private float aZ;
	private float rX;
	private float rY;
	private float rZ;
	private float mX;
	private float mY;
	private float mZ;
	
	//data stored for plotting
	private static final int HISTORY_LENGTH = 20; 
	private int count;
	private XYSeries aX_history;
	private XYSeries aY_history;
	private XYSeries aZ_history;
	private XYSeries rX_history;
	private XYSeries rY_history;
	private XYSeries rZ_history;
	private XYSeries mX_history;
	private XYSeries mY_history;
	private XYSeries mZ_history;
	
	public ImuPanel() {
		super("IMU", BAUDRATE, HEADER, HEADER_LEN);
		this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		
		if (!this.isConnected()) return;
		
		super.addListener(new ImuListener());

		//sensor history for display
		aX_history = new XYSeries("First");
		aY_history = new XYSeries("Second");
		aZ_history = new XYSeries("Thirty");
		rX_history = new XYSeries("First");
		rY_history = new XYSeries("Second");
		rZ_history = new XYSeries("Thirty");
		mX_history = new XYSeries("First");
		mY_history = new XYSeries("Second");
		mZ_history = new XYSeries("Thrty");
		count = 0;
		
		//odom
		final XYDataset dataset = createDataset();
        final JFreeChart chart = createOdomChart(dataset);
        final ChartPanel odomChartPanel = new ChartPanel(chart);
//        odomChartPanel.setPreferredSize(new java.awt.Dimension(200, 200));
        this.add(odomChartPanel);
       
        //imu_rotX
        final XYDataset dataset1 = createDataset();
        final JFreeChart chart1 = createIMURotXChart(dataset1);
        final ChartPanel imuRotXChartPanel = new ChartPanel(chart1);
//        imuRotXChartPanel.setPreferredSize(new java.awt.Dimension(200, 270));
        this.add(imuRotXChartPanel);
        
        //comand_angle
        final XYDataset  dataset2 = createDataset();
        final JFreeChart chart2 = createCommandAngleChart(dataset2);
        final ChartPanel commandAngleChartPanel = new ChartPanel(chart2);
        this.add(commandAngleChartPanel);
	}
	
	private void logData() {
		if (Gui.GetPlayPauseState()) {
			RobotLogger rl = RobotLogger.getInstance();
		    Date now = new Date();
		    long time_in_millis = now.getTime();
		    float[] acc = new float[3];
		    float[] gyro = new float[3];
		    float[] compass = new float[3];
		    acc[0] = aX; acc[1] = aY; acc[2] = aZ;
		    gyro[0] = rX; gyro[1] = rY; gyro[2] = rZ;
		    compass[0] = mX; compass[1] = mY; compass[2] = mZ;
		   
		    if(rl == null){
		    	System.out.println("r1 is null the system is exiting\n");
		        System.exit(1);
		    }else if(rl.sensor == null){
		    	System.out.println("rl.sensor is null the system is exiting\n");
		    	System.exit(1);
		    }
		    
		    rl.sensor.logImu(time_in_millis, acc, gyro, compass);
		}
	}
	
	private void addToDisplay() {
		addToHistory(aX_history,Double.valueOf(aX));
	    addToHistory(aY_history,Double.valueOf(aY));
	    addToHistory(aZ_history,Double.valueOf(aZ));
	    addToHistory(rX_history,Double.valueOf(rX));
	    addToHistory(rY_history,Double.valueOf(rY));
	    addToHistory(rZ_history,Double.valueOf(rZ));
	    addToHistory(mX_history,Double.valueOf(mX));
	    addToHistory(mY_history,Double.valueOf(mY));
	    addToHistory(mZ_history,Double.valueOf(mZ));
	    
	    count++;
	}

	/**
	 * ImuListener is an event handler for serial communication. It is notified
	 * every time a complete message is received by serial port for the given
	 * panel. It handles the serial event and parses the data to update the
	 * current properties of the given panel.
	 */
	private class ImuListener implements SerialListener {
		@Override
		public void onEvent(SerialEvent event) {
			if(!Gui.InPlayBack() && 
					(Gui.GetGraphState() || Gui.GetPlayPauseState())) {
				char[] tmp = event.getBuffer();
				int index = 0;
	
				if (tmp != null && event.getLength() > HEADER_LEN) {
					String curVal = "";
					for (int i = HEADER_LEN; i < event.getLength(); i++ ) {
						if(tmp[i] == '\n'){
							break;
						}
						else if (tmp[i] == ',' ){ 
							try {
								setValue(index, Float.valueOf(curVal));
								
								curVal = "";
								index++;
							} catch (Exception e) {
								System.out.println("Failed to parse IMU message");
								return;
							}
							
						} else {
							curVal += tmp[i];
						}
					}
					System.out.format("IMU Values: aX: %f aY: %f aZ: %f rX: %f rY: %f rZ: %f "
							+ "mX: %f mY: %f mZ: %f \n",aX,aY,aZ,rX,rY,rZ,mX,mY,mZ);
					logData();
					addToDisplay();
				}
			}
		}
	}
	
	private void addToHistory(XYSeries history,double newdata) {
	    if(history.getItemCount() > HISTORY_LENGTH) {
	    	history.remove(0);
	    }
	    history.add(count,newdata);
	}
	
	private void setValue(int index, float value) {
		switch ( index ) {
		case AX:
			aX = value;
			break;
		case AY:
			aY = value;
			break;
		case AZ:
			aZ = value;
			Gui.UpdateRobotAccel(aX, aY, aZ);
			break;
		case RX:
			rX = value;
			break;
		case RY:
			rY = value;
			break;
		case RZ:
			rZ = value;
			Gui.UpdateRobotGyro(rX, rY, rZ);
			break;
		case MX:
			mX = value;
			break;
		case MY:
			mY = value;
			break;
		case MZ:
			mZ = value;
			Gui.UpdateRobotMagnet(mX, mY, mZ);
			break;
		default:
			return;
		}
	}
    	
	XYDataset createDataset() {

	    final XYSeriesCollection dataset = new XYSeriesCollection();
	    dataset.addSeries(aX_history);
	    dataset.addSeries(aY_history);
	    dataset.addSeries(aZ_history);
	            
	    return dataset;
	    
	}

	private JFreeChart createIMURotXChart(final XYDataset dataset){
	    
	 // create the chart...
	 final JFreeChart chart = ChartFactory.createXYLineChart(
	     "IMU X rot",      // chart title
	     "time (s)",                      // x axis label
	     "radians",                      // y axis label
	     dataset,                  // data
	     PlotOrientation.VERTICAL,
	     true,                     // include legend
	     true,                     // tooltips
	     false                     // urls
	 );

	 // NOW DO SOME OPTIONAL CUSTOMISATION OF THE CHART...
	 chart.setBackgroundPaint(Color.white);

	// final StandardLegend legend = (StandardLegend) chart.getLegend();
	//  legend.setDisplaySeriesShapes(true);
	 
	 // get a reference to the plot for further customisation...
	 final XYPlot plot = chart.getXYPlot();
	 plot.setBackgroundPaint(Color.lightGray);
//	    plot.setAxisOffset(new Spacer(Spacer.ABSOLUTE, 5.0, 5.0, 5.0, 5.0));
	 plot.setDomainGridlinePaint(Color.white);
	 plot.setRangeGridlinePaint(Color.white);
	 
	 final XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();
	 renderer.setSeriesLinesVisible(0, false);
	 renderer.setSeriesShapesVisible(1, false);
	 plot.setRenderer(renderer);

	 // change the auto tick unit selection to integer units only...
	 final NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
	 rangeAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());
	 // OPTIONAL CUSTOMISATION COMPLETED.
	         
	 return chart;
	 
	}

	private JFreeChart createCommandAngleChart(final XYDataset dataset) {
	 
	 // create the chart...
	 final JFreeChart chart = ChartFactory.createXYLineChart(
	     "Command Angle",      // chart title
	     "time",                      // x axis label
	     "degrees",                      // y axis label
	     dataset,                  // data
	     PlotOrientation.VERTICAL,
	     true,                     // include legend
	     true,                     // tooltips
	     false                     // urls
	 );

	 // NOW DO SOME OPTIONAL CUSTOMISATION OF THE CHART...
	 chart.setBackgroundPaint(Color.white);

	// final StandardLegend legend = (StandardLegend) chart.getLegend();
	//  legend.setDisplaySeriesShapes(true);
	 
	 // get a reference to the plot for further customisation...
	 final XYPlot plot = chart.getXYPlot();
	 plot.setBackgroundPaint(Color.lightGray);
//	    plot.setAxisOffset(new Spacer(Spacer.ABSOLUTE, 5.0, 5.0, 5.0, 5.0));
	 plot.setDomainGridlinePaint(Color.white);
	 plot.setRangeGridlinePaint(Color.white);
	 
	 final XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();
	 renderer.setSeriesLinesVisible(0, false);
	 renderer.setSeriesShapesVisible(1, false);
	 plot.setRenderer(renderer);

	 // change the auto tick unit selection to integer units only...
	 final NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
	 rangeAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());
	 // OPTIONAL CUSTOMISATION COMPLETED.
	         
	 return chart;
	 
	}
	
	private JFreeChart createOdomChart(final XYDataset dataset) {
	    
	    // create the chart...
	    final JFreeChart chart = ChartFactory.createXYLineChart(
	        "Omom ticks",      // chart title
	        "time",                      // x axis label
	        "mag",                      // y axis label
	        dataset,                  // data
	        PlotOrientation.VERTICAL,
	        true,                     // include legend
	        true,                     // tooltips
	        false                     // urls
	    );

	    // NOW DO SOME OPTIONAL CUSTOMISATION OF THE CHART...
	    chart.setBackgroundPaint(Color.white);

//	    final StandardLegend legend = (StandardLegend) chart.getLegend();
//	      legend.setDisplaySeriesShapes(true);
	    
	    // get a reference to the plot for further customisation...
	    final XYPlot plot = chart.getXYPlot();
	    plot.setBackgroundPaint(Color.lightGray);
//	    plot.setAxisOffset(new Spacer(Spacer.ABSOLUTE, 5.0, 5.0, 5.0, 5.0));
	    plot.setDomainGridlinePaint(Color.white);
	    plot.setRangeGridlinePaint(Color.white);
	    
	    final XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();
	    renderer.setSeriesLinesVisible(0, false);
	    renderer.setSeriesShapesVisible(1, false);
	    plot.setRenderer(renderer);

	    NumberAxis range = (NumberAxis) plot.getRangeAxis();
        range.setRange(0.0, 20.0);
	    // change the auto tick unit selection to integer units only...
	    final NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
	    rangeAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());
	    // OPTIONAL CUSTOMISATION COMPLETED.
	         
	    return chart;
	    
	}
}