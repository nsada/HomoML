package test.semeionExample;

import basicUtils.ContourExtractor;
import basicUtils.Matrix;
import dataInterface.BasicDataProvider;
import dataInterface.DataProvider;
import dataInterface.semeionInterfaces.SemeionDataProvider;
import examples.RBFKernel;
import mlalgorithms.SupportVectorMachine;
import dataInterface.semeionInterfaces.OneNumberSensitiveSemeion;

/**
 * Created by Xiangxi on 2016/12/27.
 * Contact him on zxx_1996@qq.com
 */
public class Test {
    public static void main(String[] args) {
        SemeionDataProvider semeionDataProvider = new OneNumberSensitiveSemeion("data\\semeion.data", new ContourExtractor(16,16),4);

        Matrix fullDataMatrix = semeionDataProvider.getFeatureMatrix();
        int total = fullDataMatrix.getHeight();
        double sampleRate = 0.3;
        int trainNumber = (int)(total * sampleRate);
        int testNumber = total - trainNumber;

        DataProvider trainDataProvider = new BasicDataProvider(fullDataMatrix.subMatrix(0, 0,
                fullDataMatrix.getWidth(), trainNumber), semeionDataProvider.getLabelMatrix().subMatrix(0, 0,
                1, trainNumber));
        DataProvider testDataProvider = new BasicDataProvider(
                fullDataMatrix.subMatrix(0, trainNumber, fullDataMatrix.getWidth(), total),
                semeionDataProvider.getLabelMatrix().subMatrix(0, trainNumber, 1, total)
        );

        SupportVectorMachine supportVectorMachine = new SupportVectorMachine(trainDataProvider, new RBFKernel(2.73));
        supportVectorMachine.train();
        supportVectorMachine.saveRbfModelToFile("data\\test\\semeionSave.data");
        SupportVectorMachine refinedModel = new SupportVectorMachine("data\\test\\semeionSave.data");
        double[][] testResult = new double[testNumber][1];
        for(int i = 0; i < testNumber; i++) {
            testResult[i][0] = refinedModel.test(testDataProvider.getDataMatrix().get(i));
        }
        System.out.println("The error rate is about " + testErrorRate(new Matrix(testResult), testDataProvider.getLabelMatrix()));
    }

    private static double testErrorRate(Matrix result, Matrix expected) {
        double sum = 0;
        int length = result.getHeight();
        for(int i = 0; i < length; i++) {
            if (result.get(i,0) * expected.get(i,0) < 0)
                sum += 1;
        }
        return sum / length;
    }
}
