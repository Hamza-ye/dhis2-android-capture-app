package org.dhis2.usescases.datasets.datasetInitial;

import android.os.Bundle;
import android.view.Gravity;
import android.view.Menu;
import android.view.View;
import android.widget.PopupMenu;

import com.google.android.material.textfield.TextInputEditText;

import org.dhis2.App;
import org.dhis2.R;
import org.dhis2.databinding.ActivityDatasetInitialBinding;
import org.dhis2.databinding.ItemCategoryComboBinding;
import org.dhis2.usescases.general.ActivityGlobalAbstract;
import org.dhis2.utils.Constants;
import org.dhis2.utils.DateUtils;
import org.dhis2.utils.custom_views.OrgUnitDialog;
import org.dhis2.utils.custom_views.PeriodDialog;
import org.dhis2.utils.custom_views.PeriodDialogInputPeriod;
import org.hisp.dhis.android.core.category.CategoryModel;
import org.hisp.dhis.android.core.category.CategoryOptionModel;
import org.hisp.dhis.android.core.organisationunit.OrganisationUnitModel;
import org.hisp.dhis.android.core.period.PeriodType;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import javax.inject.Inject;

import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;

public class DataSetInitialActivity extends ActivityGlobalAbstract implements DataSetInitialContract.View {

    private ActivityDatasetInitialBinding binding;
    View selectedView;
    @Inject
    DataSetInitialContract.Presenter presenter;

    private HashMap<String, CategoryOptionModel> selectedCatOptions;
    private OrganisationUnitModel selectedOrgUnit;
    private Date selectedPeriod;
    private String dataSetUid;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        dataSetUid = getIntent().getStringExtra(Constants.DATA_SET_UID);
        ((App) getApplicationContext()).userComponent().plus(new DataSetInitialModule(dataSetUid)).inject(this);

        binding = DataBindingUtil.setContentView(this, R.layout.activity_dataset_initial);
        binding.setPresenter(presenter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        presenter.init(this);
    }

    @Override
    protected void onPause() {
        presenter.onDettach();
        super.onPause();
    }

    @Override
    public void setAccessDataWrite(Boolean canWrite) {

    }

    @Override
    public void setData(DataSetInitialModel dataSetInitialModel) {
        binding.setDataSetModel(dataSetInitialModel);
        binding.catComboContainer.removeAllViews();
        selectedCatOptions = new HashMap<>();
        if (!dataSetInitialModel.categoryComboName().equals("default"))
            for (CategoryModel categoryModel : dataSetInitialModel.categories()) {
                selectedCatOptions.put(categoryModel.uid(), null);
                ItemCategoryComboBinding categoryComboBinding = ItemCategoryComboBinding.inflate(getLayoutInflater(), binding.catComboContainer, false);
                categoryComboBinding.inputLayout.setHint(categoryModel.displayName());
                categoryComboBinding.inputEditText.setOnClickListener(view -> {
                    selectedView = view;
                    presenter.onCatOptionClick(categoryModel.uid());
                });
                binding.catComboContainer.addView(categoryComboBinding.getRoot());
            }
        else
            presenter.onCatOptionClick(dataSetInitialModel.categories().get(0).uid());
        checkActionVisivbility();
    }

    /**
     * When changing orgUnit, date must be cleared
     */
    @Override
    public void showOrgUnitDialog(List<OrganisationUnitModel> data) {
        OrgUnitDialog orgUnitDialog = OrgUnitDialog.getInstace().setMultiSelection(false);
        orgUnitDialog.setOrgUnits(data);
        orgUnitDialog.setTitle(getString(R.string.org_unit))
                .setPossitiveListener(v -> {
                    if (orgUnitDialog.getSelectedOrgUnit() != null && !orgUnitDialog.getSelectedOrgUnit().isEmpty()) {
                        selectedOrgUnit = orgUnitDialog.getSelectedOrgUnitModel();
                        if (selectedOrgUnit == null)
                            orgUnitDialog.dismiss();
                        binding.dataSetOrgUnitEditText.setText(selectedOrgUnit.displayName());
                        binding.dataSetPeriodEditText.setText("");
                    }
                    checkActionVisivbility();
                    orgUnitDialog.dismiss();
                })
                .setNegativeListener(v -> orgUnitDialog.dismiss());
        orgUnitDialog.show(getSupportFragmentManager(), OrgUnitDialog.class.getSimpleName());
    }

    @Override
    public void showPeriodSelector(PeriodType periodType, List<DateRangeInputPeriodModel> periods) {
        new PeriodDialogInputPeriod()
                .setInputPeriod(periods)
                .setPeriod(periodType)
//                .setMinDate() TODO: Depends on dataSet expiration settings and orgUnit Opening date
//                .setMaxDate() TODO: Depends on dataSet open Future settings. Default: TODAY
                .setMaxDate(DateUtils.getInstance().getNextPeriod(periodType, DateUtils.getInstance().getToday(), -1))
                .setPossitiveListener(selectedDate -> {
                    Calendar calendar = Calendar.getInstance();
                    calendar.setTime(selectedDate);
                    calendar.set(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.getActualMaximum(Calendar.DAY_OF_MONTH));
                    this.selectedPeriod = calendar.getTime();
                    binding.dataSetPeriodEditText.setText(DateUtils.getInstance().getPeriodUIString(periodType, selectedDate, Locale.getDefault()));
                    checkActionVisivbility();
                })
                .show(getSupportFragmentManager(), PeriodDialog.class.getSimpleName());
    }

    @Override
    public void showCatComboSelector(String catOptionUid, List<CategoryOptionModel> data) {
        if (data.size() == 1 && data.get(0).name().equals("default")) {
            if (selectedCatOptions == null)
                selectedCatOptions = new HashMap<>();
            selectedCatOptions.put(catOptionUid, data.get(0));
        } else {

            PopupMenu menu = new PopupMenu(this, selectedView, Gravity.BOTTOM);
//        menu.getMenu().add(Menu.NONE, Menu.NONE, 0, viewModel.label()); Don't show label
            for (CategoryOptionModel optionModel : data)
                menu.getMenu().add(Menu.NONE, Menu.NONE, data.indexOf(optionModel), optionModel.displayName());

            menu.setOnDismissListener(menu1 -> selectedView = null);
            menu.setOnMenuItemClickListener(item -> {
                if (selectedCatOptions == null)
                    selectedCatOptions = new HashMap<>();
                selectedCatOptions.put(catOptionUid, data.get(item.getOrder()));
                ((TextInputEditText) selectedView).setText(data.get(item.getOrder()).displayName());
                checkActionVisivbility();
                return false;
            });
            menu.show();
        }
    }

    @Override
    public String getDataSetUid() {
        return dataSetUid;
    }

    @Override
    public OrganisationUnitModel getSelectedOrgUnit() {
        return selectedOrgUnit;
    }

    @Override
    public Date getSelectedPeriod() {
        return selectedPeriod;
    }

    @Override
    public String getSelectedCatOptions() {
        StringBuilder catComb = new StringBuilder("'");
        for (int i = 0; i < selectedCatOptions.keySet().size(); i++) {
            CategoryOptionModel catOpt = selectedCatOptions.get(selectedCatOptions.keySet().toArray()[i]);
            catComb.append(catOpt.uid());

            if (i < selectedCatOptions.values().size() - 1)
                catComb.append("', '");
        }
        return catComb.append("'").toString();
    }

    @Override
    public String getPeriodType() {
        return binding.getDataSetModel().periodType().name();
    }

    private void checkActionVisivbility() {
        boolean visible = true;
        if (selectedOrgUnit == null)
            visible = false;
        if (selectedPeriod == null)
            visible = false;
        for (String key : selectedCatOptions.keySet()) {
            if (selectedCatOptions.get(key) == null)
                visible = false;
        }

        binding.actionButton.setVisibility(visible ? View.VISIBLE : View.GONE);

    }

    @Override
    public Date getPeriodDate() {
        return selectedPeriod;
    }
}