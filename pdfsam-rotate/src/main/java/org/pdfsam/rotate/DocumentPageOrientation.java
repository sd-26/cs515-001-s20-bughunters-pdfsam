package org.pdfsam.rotate;

import org.pdfsam.task.PdfRotationInput;
import org.sejda.commons.collection.NullSafeSet;
import org.sejda.impl.sambox.component.DefaultPdfSourceOpener;
import org.sejda.impl.sambox.component.PDDocumentHandler;
import org.sejda.model.exception.TaskIOException;
import org.sejda.model.input.PdfSource;
import org.sejda.model.pdf.page.PageRange;
import org.sejda.model.pdf.page.PagesSelection;
import org.sejda.model.pdf.page.PredefinedSetOfPages;
import org.sejda.model.rotation.Rotation;

import java.util.*;

import static java.util.Objects.isNull;

public class DocumentPageOrientation {
    private Set<PdfRotationInput> inputs = new NullSafeSet<>();
    private Rotation rotation;
    private PredefinedSetOfPages predefinedRotationType;

    public DocumentPageOrientation(Rotation rotation, PredefinedSetOfPages predefinedRotationType) {
        this.rotation = rotation;
        this.predefinedRotationType = predefinedRotationType;
    }

    public Set<PdfRotationInput> getInputs() {
        return inputs;
    }

    void addInput(PdfSource<?> source, Set<PageRange> pageSelection) {
        if (isNull(pageSelection) || pageSelection.isEmpty()) {
            this.inputs.add(new PdfRotationInput(source, rotation, predefinedRotationType));
        } else {
            this.inputs.add(new PdfRotationInput(source, rotation, getPageCombination(source,
                    pageSelection.toArray(new PageRange[0]))));
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
                    int rangeEnd = Math.min(range.getEnd(), totalPages);
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
}
