package network.radicle.jetbrains.radiclejetbrainsplugin.toolwindow;

import com.intellij.openapi.ui.popup.JBPopupListener;
import com.intellij.openapi.ui.popup.LightweightWindowEvent;
import com.intellij.ui.awt.RelativePoint;
import com.intellij.ui.scale.JBUIScale;
import com.intellij.util.ui.FilterComponent;
import com.intellij.util.ui.UIUtil;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import kotlin.coroutines.CoroutineContext;
import kotlinx.coroutines.flow.FlowCollector;
import kotlinx.coroutines.flow.MutableStateFlow;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComponent;
import javax.swing.JList;
import java.awt.Point;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;

public class DropdownFilter extends FilterComponent {
    private final String filterName;
    private final MutableStateFlow<String> state;
    private final CoroutineContext coroutineContext;

    public DropdownFilter(String filterName, MutableStateFlow<String> state, CoroutineContext coroutineContext) {
        super(() -> filterName);
        this.state = state;
        this.coroutineContext = coroutineContext;
        this.filterName = filterName;
    }

    @Override
    public @NotNull @Nls String getCurrentText() {
        var value = (String) state.getValue();
        return value != null ? value : getEmptyFilterValue();
    }

    @Override
    public @NotNull @Nls String getEmptyFilterValue() {
        return "";
    }

    @Override
    protected boolean isValueSelected() {
        return state.getValue() != null;
    }

    @Override
    public void installChangeListener(@NotNull Runnable onChange) {
        state.collect(new FlowCollector<>() {
            @Nullable
            @Override
            public Object emit(String s, @NotNull Continuation<? super Unit> continuation) {
                onChange.run();
                return null;
            }
        }, new Continuation<Object>() {
            @NotNull
            @Override
            public CoroutineContext getContext() {
                return coroutineContext;
            }

            @Override
            public void resumeWith(@NotNull Object o) {

            }
        });
    }

    @NotNull
    @Override
    protected DrawLabelMode shouldDrawLabel() {
        return DrawLabelMode.WHEN_VALUE_NOT_SET;
    }

    @Override
    protected Runnable createResetAction() {
        return () -> {
            var prevValue = state.getValue();
            state.compareAndSet(prevValue, null);
        };
    }

    public MutableStateFlow<String> getState() {
        return this.state;
    }

    public void showPopup(CompletableFuture<List<String>> data, CountDownLatch latch) {
        var point = new RelativePoint(this, new Point(0, this.getHeight() + JBUIScale.scale(4)));
        var popUpBuilder = new PopupBuilder();
        var popup = popUpBuilder.createPopup(data, latch);
        popup.show(point);
        var list = UIUtil.findComponentOfType(popup.getContent(), JList.class);
        popup.addListener(new JBPopupListener() {
            @Override
            public void onClosed(@NotNull LightweightWindowEvent event) {
                if (event.isOk()) {
                    var prevValue = state.getValue();
                    var nextValue = (String) list.getSelectedValue();
                    state.compareAndSet(prevValue, nextValue);
                }
            }
        });
    }

    public JComponent init() {
        var filter = this.initUi();
        UIUtil.setTooltipRecursively(filter, filterName);
        return filter;
    }
}
