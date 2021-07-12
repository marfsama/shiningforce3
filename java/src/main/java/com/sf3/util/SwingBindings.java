/**
 *
 */
package com.sf3.util;

import javax.swing.JSlider;

import com.jgoodies.binding.adapter.Bindings;
import com.jgoodies.binding.adapter.BoundedRangeAdapter;
import com.jgoodies.binding.value.ValueModel;

/**
 * some extensions of {@link Bindings}
 */
public class SwingBindings
{
  /**
   * Binds the given JSlider to the specified ValueModel.
   * Synchronized the ValueModel's value with the sliders field's value
   * by means of a PropertyConnector.
   *
   * @param slider   the JSlider
   * @param valueModel  the model that provides the value
   * @param min minimum slider value
   * @param max maximum slider value
   */
  public static void bind(JSlider slider, ValueModel valueModel, int min, int max)
  {
    // Bindings.bind(textField, "value", valueModel);
    slider.setModel(new BoundedRangeAdapter(valueModel, 0, min, max));
  }

  /**
   * Creates and returns a slider that is bound to the given ValueModel.
   */
  public static JSlider createSlider(ValueModel valueModel, int orientation, int min, int max)
  {
    JSlider slider = new JSlider(orientation);
    bind(slider, valueModel, min, max);
    return slider;
  }
}
