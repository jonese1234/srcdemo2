package net.srcdemo.ui;

import java.io.File;
import java.util.HashSet;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicInteger;

import net.srcdemo.RollingRate;
import net.srcdemo.SrcDemoListener;
import net.srcdemo.Strings;
import net.srcdemo.audio.BufferedAudioHandler.AudioBufferStatus;

import com.trolltech.qt.core.QCoreApplication;
import com.trolltech.qt.core.QEvent;
import com.trolltech.qt.core.QObject;
import com.trolltech.qt.core.Qt.AlignmentFlag;
import com.trolltech.qt.core.Qt.Orientation;
import com.trolltech.qt.gui.QCheckBox;
import com.trolltech.qt.gui.QGroupBox;
import com.trolltech.qt.gui.QHBoxLayout;
import com.trolltech.qt.gui.QLabel;
import com.trolltech.qt.gui.QProgressBar;
import com.trolltech.qt.gui.QPushButton;
import com.trolltech.qt.gui.QSizePolicy.Policy;
import com.trolltech.qt.gui.QVBoxLayout;
import com.trolltech.qt.gui.QWidget;

class RenderingTab extends QWidget implements SrcDemoListener {
	private static final int maximumPreviewHeight = 480;
	private static final int maximumPreviewWidth = 640;
	/**
	 * Time between UI updates, in milliseconds
	 */
	private static final long uiUpdateInterval = 750;
	private QProgressBar audioBuffer;
	private final Runnable audioBufferClosed = new Runnable() {
		@Override
		public void run() {
			lblAudioBuffer1.setText(Strings.lblRenderAudioBufferClosed);
			lblAudioBuffer2.setText("");
			btnFlushAudioBuffer.setText(Strings.btnRenderAudioBufferFlush);
			enabledAudioWidgets(false);
		}
	};
	private final Runnable audioBufferFlushEnd = new Runnable() {
		@Override
		public void run() {
			lblAudioBuffer1.setText(Strings.lblRenderAudioBufferWritten);
			lblAudioBuffer2.setText("");
			btnFlushAudioBuffer.setText(Strings.btnRenderAudioBufferFlushed);
		}
	};
	private final Runnable audioBufferFlushStart = new Runnable() {
		@Override
		public void run() {
			lblAudioBuffer1.setText(Strings.lblRenderAudioBufferWriting);
			lblAudioBuffer2.setText("");
			btnFlushAudioBuffer.setText(Strings.btnRenderAudioBufferFlushing);
			enabledAudioWidgets(false);
		}
	};
	private boolean audioBufferInUse = false;
	private int audioBufferOccupied = 0;
	private AudioBufferStatus audioBufferStatus = AudioBufferStatus.DESTROYED;
	private int audioBufferTotal = 1;
	private final Set<QWidget> audioWidgets = new HashSet<QWidget>();
	private QPushButton btnFlushAudioBuffer;
	private final QObject focusEventFilter = new QObject() {
		@Override
		public boolean eventFilter(final QObject obj, final QEvent event) {
			switch (event.type()) {
				case ApplicationActivate:
					onFocus();
					break;
				case ApplicationDeactivate:
					onDefocus();
					break;
				default:
			}
			return super.eventFilter(obj, event);
		}
	};
	private final AtomicInteger framesProcessed = new AtomicInteger(0);
	private final RollingRate framesProcessRate = new RollingRate();
	private final AtomicInteger framesSaved = new AtomicInteger(0);
	private boolean hasFocus = true;
	private QLabel lblAudioBuffer1;
	private QLabel lblAudioBuffer2;
	private QLabel lblFramesProcessedPerSecond;
	private QLabel lblLastFrameProcessed;
	private QLabel lblLastFrameSaved;
	private final SrcDemoUI parent;
	private boolean previewEnabled;
	private QCheckBox previewEnabledCheckbox;
	private UpdatablePicture previewPicture;

	RenderingTab(final SrcDemoUI parent) {
		this.parent = parent;
		initUI();
		parent.getQApplication().installEventFilter(focusEventFilter);
		final Runnable updateUi = new Runnable() {
			@Override
			public void run() {
				updateUI();
			}
		};
		new Timer("Rendering tab updater", true).schedule(new TimerTask() {
			@Override
			public void run() {
				QCoreApplication.invokeLater(updateUi);
			}
		}, 0, uiUpdateInterval);
	}

	private QWidget audioWidget(final QWidget widget) {
		audioWidgets.add(widget);
		return widget;
	}

	private void enabledAudioWidgets(final boolean enable) {
		for (final QWidget w : audioWidgets) {
			w.setEnabled(enable);
		}
	}

	private SrcSettings getSettings() {
		return parent.getSettings();
	}

	private void initUI() {
		final QHBoxLayout mainHbox = new QHBoxLayout();
		{
			final QGroupBox videoBox = new QGroupBox(Strings.grpRenderingVideoFrames);
			final QVBoxLayout videoVbox = new QVBoxLayout();
			{
				final QHBoxLayout hbox = new QHBoxLayout();
				hbox.addWidget(new QLabel(Strings.lblLastFrameProcessed));
				lblLastFrameProcessed = new QLabel(Strings.lblLastFrameProcessedDefault);
				lblLastFrameProcessed.setAlignment(AlignmentFlag.AlignRight);
				hbox.addWidget(lblLastFrameProcessed);
				videoVbox.addLayout(hbox, 0);
			}
			{
				final QHBoxLayout hbox = new QHBoxLayout();
				hbox.addWidget(new QLabel(Strings.lblFramesProcessedPerSecond));
				lblFramesProcessedPerSecond = new QLabel(Strings.lblFramesProcessedPerSecondDefault);
				lblFramesProcessedPerSecond.setAlignment(AlignmentFlag.AlignRight);
				hbox.addWidget(lblFramesProcessedPerSecond);
				videoVbox.addLayout(hbox, 0);
			}
			{
				final QHBoxLayout hbox = new QHBoxLayout();
				hbox.addWidget(new QLabel(Strings.lblLastFrameSaved));
				lblLastFrameSaved = new QLabel(Strings.lblLastFrameSavedDefault);
				lblLastFrameSaved.setAlignment(AlignmentFlag.AlignRight);
				hbox.addWidget(lblLastFrameSaved);
				videoVbox.addLayout(hbox, 0);
			}
			{
				previewPicture = new UpdatablePicture(Files.iconRenderingDefault, maximumPreviewWidth, maximumPreviewHeight);
				videoVbox.addWidget(previewPicture, 1);
			}
			{
				previewEnabledCheckbox = new QCheckBox(Strings.lblEnablePreview);
				previewEnabledCheckbox.stateChanged.connect(this, "updatePreviewEnabled()");
				previewEnabled = getSettings().getPreviewEnabled();
				previewEnabledCheckbox.setChecked(previewEnabled);
				videoVbox.addWidget(previewEnabledCheckbox, 0, AlignmentFlag.AlignHCenter);
			}
			videoBox.setLayout(videoVbox);
			mainHbox.addWidget(videoBox, 1);
		}
		{
			final QGroupBox audioBox = new QGroupBox(Strings.grpRenderingAudioBuffer);
			final QVBoxLayout audioVbox = new QVBoxLayout();
			{
				audioBuffer = new QProgressBar();
				audioBuffer.setOrientation(Orientation.Vertical);
				audioBuffer.setSizePolicy(Policy.Expanding, Policy.Expanding);
				audioVbox.addWidget(audioWidget(audioBuffer), 1, AlignmentFlag.AlignHCenter);
				lblAudioBuffer1 = new QLabel();
				audioVbox.addWidget(audioWidget(lblAudioBuffer1), 0, AlignmentFlag.AlignCenter);
				lblAudioBuffer2 = new QLabel();
				audioVbox.addWidget(audioWidget(lblAudioBuffer2), 0, AlignmentFlag.AlignCenter);
				btnFlushAudioBuffer = new QPushButton(Strings.btnRenderAudioBufferFlush);
				btnFlushAudioBuffer.clicked.connect(this, "onFlushAudioBuffer()");
				btnFlushAudioBuffer.setEnabled(false);
				audioVbox.addWidget(audioWidget(btnFlushAudioBuffer), 0, AlignmentFlag.AlignCenter);
			}
			audioBox.setLayout(audioVbox);
			mainHbox.addWidget(audioBox, 0);
		}
		setLayout(mainHbox);
	}

	@Override
	public void onAudioBuffer(final AudioBufferStatus status, final int occupied, final int total) {
		audioBufferOccupied = occupied;
		audioBufferTotal = total;
		switch (status) {
			case REGULAR:
				audioBufferInUse = true;
				if (audioBufferStatus.equals(AudioBufferStatus.FLUSHING)) {
					// End of flush
					QCoreApplication.invokeLater(audioBufferFlushEnd);
				}
				break;
			case FLUSHING:
				audioBufferInUse = true;
				QCoreApplication.invokeLater(audioBufferFlushStart);
				break;
			case DESTROYED:
				audioBufferInUse = false;
				QCoreApplication.invokeLater(audioBufferClosed);
				break;
		}
		audioBufferStatus = status;
	}

	private void onDefocus() {
		hasFocus = false;
		if (previewEnabled) {
			previewPicture.reset();
		}
	}

	@SuppressWarnings("unused")
	private void onFlushAudioBuffer() {
		audioBufferStatus = AudioBufferStatus.FLUSHING;
		enabledAudioWidgets(false);
		btnFlushAudioBuffer.setText(Strings.btnRenderAudioBufferFlushing);
		parent.flushAudioBuffer(false);
	}

	private void onFocus() {
		hasFocus = true;
		if (previewEnabled) {
			previewPicture.restore();
		}
	}

	@Override
	public void onFrameProcessed(final String frameName) {
		framesProcessed.incrementAndGet();
		framesProcessRate.mark();
	}

	@Override
	public void onFrameSaved(final File savedFrame, final int[] pixels, final int width, final int height) {
		framesSaved.incrementAndGet();
		if (previewEnabled && hasFocus) {
			previewPicture.push(pixels, width, height);
		}
	}

	@SuppressWarnings("unused")
	private void updatePreviewEnabled() {
		previewEnabled = previewEnabledCheckbox.isChecked();
		getSettings().setPreviewEnabled(previewEnabled);
		if (!previewEnabled) {
			previewPicture.reset();
		}
		previewPicture.updatePicture();
	}

	private void updateUI() {
		lblLastFrameProcessed.setText(Integer.toString(framesProcessed.get()));
		lblFramesProcessedPerSecond.setText(framesProcessRate.getFormattedRate());
		lblLastFrameSaved.setText(Integer.toString(framesSaved.get()));
		if (previewEnabled) {
			previewPicture.updatePicture();
		}
		if (audioBufferInUse && parent.isAudioBufferInUse()) {
			switch (audioBufferStatus) {
				case REGULAR:
					audioBuffer.setMaximum(audioBufferTotal);
					audioBuffer.setValue(audioBufferOccupied);
					lblAudioBuffer1.setText((audioBufferOccupied / 1024) + Strings.lblRenderAudioBuffer1
						+ (audioBufferOccupied * 100 / audioBufferTotal) + Strings.lblRenderAudioBuffer2);
					lblAudioBuffer2.setText(Strings.lblRenderAudioBuffer3 + (audioBufferTotal / 1024)
						+ Strings.lblRenderAudioBuffer4);
					btnFlushAudioBuffer.setText(Strings.btnRenderAudioBufferFlush);
					enabledAudioWidgets(audioBufferOccupied > 0 && audioBufferTotal > 0);
					break;
				case FLUSHING:
				case DESTROYED:
					audioBuffer.setMaximum(1);
					audioBuffer.setValue(0);
					btnFlushAudioBuffer.setEnabled(false);
					break;
			}
		} else {
			enabledAudioWidgets(false);
		}
	}
}
