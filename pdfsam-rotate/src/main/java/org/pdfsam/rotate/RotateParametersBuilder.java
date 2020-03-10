/* 
 * This file is part of the PDF Split And Merge source code
 * Created on 26/giu/2014
 * Copyright 2017 by Sober Lemur S.a.s. di Vacondio Andrea (info@pdfsam.org).
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as 
 * published by the Free Software Foundation, either version 3 of the 
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.pdfsam.rotate;

import static java.util.Objects.isNull;

import java.util.*;

import org.pdfsam.support.params.AbstractPdfOutputParametersBuilder;
import org.pdfsam.support.params.MultipleOutputTaskParametersBuilder;
import org.pdfsam.task.BulkRotateParameters;
import org.pdfsam.task.PdfRotationInput;
import org.sejda.commons.collection.NullSafeSet;
import org.sejda.impl.sambox.component.DefaultPdfSourceOpener;
import org.sejda.impl.sambox.component.PDDocumentHandler;
import org.sejda.model.exception.TaskIOException;

import org.sejda.model.input.PdfSource;
import org.sejda.model.output.SingleOrMultipleTaskOutput;
import org.sejda.model.pdf.page.PageRange;
import org.sejda.model.pdf.page.PagesSelection;
import org.sejda.model.pdf.page.PredefinedSetOfPages;
import org.sejda.model.rotation.Rotation;

/**
 * Builder for {@link BulkRotateParameters}
 * 
 * @author Andrea Vacondio
 *
 */
class RotateParametersBuilder extends AbstractPdfOutputParametersBuilder<BulkRotateParameters>
        implements MultipleOutputTaskParametersBuilder<BulkRotateParameters> {

    private SingleOrMultipleTaskOutput output;
    private String prefix;
    private Set<PdfRotationInput> inputs = new NullSafeSet<>();
    private Rotation rotation;
    private PredefinedSetOfPages predefinedRotationType;

    void addInput(PdfSource<?> source, Set<PageRange> pageSelection) {
        if (isNull(pageSelection) || pageSelection.isEmpty()) {
            this.inputs.add(new PdfRotationInput(source, rotation, predefinedRotationType));
        } else {
            this.inputs.add(new PdfRotationInput(source, rotation, getPageCombination(source,
                    pageSelection.stream().toArray(PageRange[]::new))));
        }
    }

    PagesSelection[] getPageCombination(PdfSource<?> source, PageRange...pages) {
        List<PagesSelection> result = new ArrayList<>();
        Set<Integer> pagestoRotate = new HashSet<>();
        try {
            PDDocumentHandler document = source.open(new DefaultPdfSourceOpener());
            int totalPages = document.getNumberOfPages();
            boolean selectEvenPages = predefinedRotationType.equals(PredefinedSetOfPages.EVEN_PAGES);
            boolean selectOddPages = predefinedRotationType.equals(PredefinedSetOfPages.ODD_PAGES);
            if (selectEvenPages || selectOddPages) {
                for(PageRange range: pages)
                {
                    int rangeEnd = range.getEnd()>totalPages? totalPages:range.getEnd();
                    for(int i = range.getStart(); i<=rangeEnd; i++)
                    {
                        if((selectEvenPages && i%2==0) || (selectOddPages && i%2!=0))
                        {
                            pagestoRotate.add(i);
                        }
                    }
                }
                for(int page: pagestoRotate){
                    result.add(new PageRange(page,page));
                }
            }
            else
                result.addAll(Arrays.asList(pages));
        } catch (TaskIOException e) {
            e.printStackTrace();
        } finally {
            return result.toArray(PagesSelection[]::new);
        }
    }

    boolean hasInput() {
        return !inputs.isEmpty();
    }

    @Override
    public void output(SingleOrMultipleTaskOutput output) {
        this.output = output;
    }

    @Override
    public void prefix(String prefix) {
        this.prefix = prefix;
    }

    protected SingleOrMultipleTaskOutput getOutput() {
        return output;
    }

    protected String getPrefix() {
        return prefix;
    }

    public void rotation(Rotation rotation) {
        this.rotation = rotation;
    }

    public void rotationType(PredefinedSetOfPages predefinedRotationType) {
        this.predefinedRotationType = predefinedRotationType;

    }

    @Override
    public BulkRotateParameters build() {
        BulkRotateParameters params = new BulkRotateParameters();
        params.setCompress(isCompress());
        params.setExistingOutputPolicy(existingOutput());
        params.setVersion(getVersion());
        params.setOutput(getOutput());
        params.setOutputPrefix(getPrefix());
        inputs.forEach(params::addInput);
        return params;
    }

}
