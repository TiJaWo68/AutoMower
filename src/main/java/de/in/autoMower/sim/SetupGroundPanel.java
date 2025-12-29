package de.in.autoMower.sim;

import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelListener;
import java.awt.geom.Line2D;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.text.DecimalFormat;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JFormattedTextField;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;

public class SetupGroundPanel extends SimulationPanel {

	protected abstract class MultiLineDrawer extends DelegatedMouseAdapter implements MouseWheelListener {
		protected MultiLine2D current = null;

		@Override
		public void mouseClicked(MouseEvent me) {
			if (me.getButton() == MouseEvent.BUTTON1)
				if (me.getClickCount() == 2 && current != null && current.getNumberOfPoints() > 2)
					handleLMDoubleClick();
				else
					try {
						Point2D p = createAffineTransform().inverseTransform(me.getPoint(), new Point2D.Double());
						current.addPoint(p);
						repaint();
					} catch (NoninvertibleTransformException ex) {
						ex.printStackTrace();
					}
		}

		protected void handleLMDoubleClick() {
			current.closePath();
		}
	}

	protected abstract class DelegatedMouseAdapter extends MouseAdapter {

		protected void activated() {
		}

		protected void deactivated() {
		}

		@Override
		public void mouseMoved(MouseEvent me) {
			try {
				Point2D p = createAffineTransform().inverseTransform(me.getPoint(), new Point2D.Double());
				model.mouseMoved(p);
			} catch (NoninvertibleTransformException e) {
				e.printStackTrace();
			}
		}
	}

	public class SetDelegateMouseAdapter extends AbstractAction {
		DelegatedMouseAdapter mouseAdapter;

		public SetDelegateMouseAdapter(String name, DelegatedMouseAdapter mouseAdapter) {
			super(name);
			this.mouseAdapter = mouseAdapter;
		}

		public void actionPerformed(ActionEvent e) {
			if (delegate != null)
				delegate.deactivated();
			delegate = mouseAdapter;
			delegate.activated();
		}
	}

	protected DelegatedMouseAdapter setBorderMA = new MultiLineDrawer() {
		MultiLine2D previousBorder = null;

		@Override
		protected void activated() {
			previousBorder = model.getBorder();
			current = new MultiLine2D(GroundModel.BORDER_COLOR);
			model.setBorder(current);
		}

		@Override
		protected void deactivated() {
			if (current.getNumberOfPoints() == 0)
				model.setBorder(previousBorder);
		}

	};

	protected DelegatedMouseAdapter addObstacleMA = new MultiLineDrawer() {

		@Override
		protected void activated() {
			current = new MultiLine2D(GroundModel.OBSTACLE_COLOR);
			model.addObstacle(current);
		}

		@Override
		protected void deactivated() {
			if (current.getNumberOfPoints() < 2)
				model.removeObstacle(current);
		}

		@Override
		protected void handleLMDoubleClick() {
			current.closePath();
			current = new MultiLine2D(GroundModel.OBSTACLE_COLOR);
			model.addObstacle(current);
		}

	};
	protected DelegatedMouseAdapter removeObstacleMA = new DelegatedMouseAdapter() {
		@Override
		public void mouseClicked(MouseEvent me) {
			try {
				Point2D tp = createAffineTransform().inverseTransform(me.getPoint(), new Point2D.Double());
				model.removeObstacle(model.getObstacle(tp));
			} catch (NoninvertibleTransformException ex) {
				ex.printStackTrace();
			}
		}
	};
	protected DelegatedMouseAdapter calibrateMA = new DelegatedMouseAdapter() {
		@Override
		public void mouseClicked(MouseEvent me) {
			try {
				Point2D tp = createAffineTransform().inverseTransform(me.getPoint(), new Point2D.Double());
				MultiLine2D obstacle = model.getObstacle(tp);
				if (obstacle != null) {
					Line2D line = obstacle.getLine2D(tp);
					if (line != null) {
						JFormattedTextField textfield = new JFormattedTextField(new DecimalFormat("ddd"));
						int result = JOptionPane.showConfirmDialog(SetupGroundPanel.this, textfield, "Enter length in cm for selected line",
								JOptionPane.OK_CANCEL_OPTION);
						if (result == JOptionPane.OK_OPTION) {
							int length = Integer.parseInt(textfield.getText());
							model.setCalibration(line, length);
						}
					}
				}
			} catch (NoninvertibleTransformException ex) {
				ex.printStackTrace();
			}
		}
	};
	protected DelegatedMouseAdapter chargingStationMA = new DelegatedMouseAdapter() {
		@Override
		public void mouseClicked(MouseEvent me) {
			try {
				Point2D tp = createAffineTransform().inverseTransform(me.getPoint(), new Point2D.Double());
				model.setChargingStation(tp);
			} catch (NoninvertibleTransformException ex) {
				ex.printStackTrace();
			}
		}
	};

	DelegatedMouseAdapter delegate = null;
	private JButton hamburgerBtn;

	public SetupGroundPanel(GroundModel model) {
		super(model);
		hamburgerBtn = new JButton(new HamburgerMenuIcon(30));
		hamburgerBtn.addActionListener(e -> {
			JPopupMenu menu = new JPopupMenu();
			menu.add(new SetDelegateMouseAdapter("set border", setBorderMA));
			menu.add(new SetDelegateMouseAdapter("new obstacle", addObstacleMA));
			menu.add(new SetDelegateMouseAdapter("remove obstacle", removeObstacleMA));
			menu.add(new SetDelegateMouseAdapter("calibrate", calibrateMA));
			menu.add(new SetDelegateMouseAdapter("Ladestation", chargingStationMA));
			menu.show(hamburgerBtn, 0, 0);
		});
		add(hamburgerBtn);
		updateHamburgerVisibility();

		MouseAdapter mouseAdapter = new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if (delegate != null)
					delegate.mouseClicked(e);
			}

			@Override
			public void mouseDragged(MouseEvent e) {
				if (delegate != null)
					delegate.mouseDragged(e);
			}

			@Override
			public void mousePressed(MouseEvent e) {
				if (delegate != null)
					delegate.mousePressed(e);
			}

			@Override
			public void mouseReleased(MouseEvent e) {
				if (delegate != null)
					delegate.mouseReleased(e);
			}

			@Override
			public void mouseMoved(MouseEvent e) {
				if (delegate != null)
					delegate.mouseMoved(e);
			}
		};
		addMouseListener(mouseAdapter);
		addMouseMotionListener(mouseAdapter);
	}

	@Override
	protected void onModelChanged() {
		updateHamburgerVisibility();
	}

	private void updateHamburgerVisibility() {
		if (hamburgerBtn != null && model != null) {
			hamburgerBtn.setVisible(model.getImage() != null);
		}
	}
}
