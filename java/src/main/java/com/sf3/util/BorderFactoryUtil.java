/**
 *
 */
package com.sf3.util;

import javax.swing.BorderFactory;
import javax.swing.border.BevelBorder;
import javax.swing.border.Border;

/**
 * Some static helper methods to create borders
 */
public class BorderFactoryUtil
{
  /**
   * creates a titled beveled border
   */
  public static Border createTitledRaisedBorder(String title)
  {
    return BorderFactory.createTitledBorder(
        BorderFactory.createCompoundBorder(
            BorderFactory.createBevelBorder(BevelBorder.RAISED),
            BorderFactory.createBevelBorder(BevelBorder.LOWERED)
        ),
        title
    );
  }
  /**
   * creates a titled beveled border
   */
  public static Border createTitledLoweredBorder(String title)
  {
    return BorderFactory.createTitledBorder(
          BorderFactory.createBevelBorder(BevelBorder.LOWERED)
        ,title
    );
  }

}
