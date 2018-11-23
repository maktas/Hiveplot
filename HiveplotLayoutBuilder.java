
package org.maktas.hiveplot;

import javax.swing.Icon;
import javax.swing.JPanel;
import org.gephi.layout.spi.Layout;
import org.gephi.layout.spi.LayoutBuilder;
import org.gephi.layout.spi.LayoutUI;
import org.openide.util.NbBundle;
import org.openide.util.lookup.ServiceProvider;


@ServiceProvider(service = LayoutBuilder.class)
public class HiveplotLayoutBuilder implements LayoutBuilder 
{
    private HiveplotLayoutUI ui = new HiveplotLayoutUI();
    
    @Override
    public String getName()
    {
        return NbBundle.getMessage(HiveplotLayout.class, "name");
    }


    @Override
    public LayoutUI getUI() 
    {
        return ui;
    }


    @Override
    public Layout buildLayout() 
    {
        return new HiveplotLayout(this);
    }
    
    /**
     * 
     */
    private static class HiveplotLayoutUI implements LayoutUI {


        @Override
        public String getDescription()
        {
            return NbBundle.getMessage(HiveplotLayout.class, "description");
        }


        @Override
        public Icon getIcon()
        {
            return null;
        }


        @Override
        public JPanel getSimplePanel(Layout layout) 
        {
            return null;
        }


        @Override
        public int getQualityRank() 
        {
            return 1;
        }


        @Override
        public int getSpeedRank() 
        {
            return 1;
        }
    }
}