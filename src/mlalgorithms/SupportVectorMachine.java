package mlalgorithms;

import basicUtils.Matrix;
import dataInterface.DataProvider;
import java.util.Collections;
import java.util.ArrayList;

public class SupportVectorMachine {
    /*
     * 一个简单的二分类SVM
     */
    private KernelFunction fKernel;  // Kernel函数，用于求高维空间中的向量内积，默认为线性核
    private DataProvider data;
    private Matrix productMatrix;
    private ArrayList<Double> alpha;
    private double b;
    private double C=1.0f;
    private static final double eps=0.0001;
    private double tol = 1e-3; // 判断收敛的边界,重要参数
    private boolean isInit=false;

    public SupportVectorMachine() {
    }

    public SupportVectorMachine(DataProvider dp) {
        data=dp;
    }
    public SupportVectorMachine(DataProvider dp,KernelFunction k) {
        data=dp;
        fKernel = k;
    }

    public void train() {
        if (!data.isReady()) {
            System.err.println("Not Enough Data");
            return ;
        }
        SMO();
        System.err.println("Train Has Finished!");
    }

    private double calE(int j) {
        // 计算ui-yi
        double f = 0.0;
        Matrix x=data.getDataMatrix().get(j);
        for (int i=0;i<alpha.size();i++) {
            f = f + alpha.get(i)*data.getLabelMatrix().get(i,0)*fKernel.cal(data.getDataMatrix().get(i),x);
        }
        f = f+b-data.getLabelMatrix().get(j,0);
        return f;
    }

    private double abs(double i) {
        if (i>0) return i;
        else return -i;
    }

    private void SMO() {
        if (!isInit) {
            Init();
        }
        int i = 0;
        int j = 0;
        while (!convergence()) {  // 未收敛就做
            // 选定第一个乘子，采用启发式算法，选定第一个处于0到C之间的乘子
            for (i=0;i<alpha.size();i++){
                double value=alpha.get(i).doubleValue();
                if (value>0 && value<C){
                    break;
                }
            }
            double Ei=calE(i);
            double maxEj=0.0;
            int maxj=0;  //采用启发式算法，确定和Ei差的绝对值最大的Ej
            for (j=0;j<alpha.size();j++) {
                if (i!=j){
                    double Ej = calE(j);
                    if (Ei*Ej<0){
                        if (abs(Ej)-maxEj>eps) {
                            maxEj=abs(Ej);
                            maxj=j;
                        }
                    }
                }
            }
            j=maxj;
            double Ej=calE(j);
            double yi = data.getLabelMatrix().get(i,0);
            double yj = data.getLabelMatrix().get(j,0);
            double oldalphai=alpha.get(i);
            double oldalphaj=alpha.get(j);
            double L, H;
            if (yi != yj) {
                L = Math.max(0, oldalphaj - oldalphai);
                H = Math.min(C, C - oldalphai + oldalphaj);
            } else {
                L = Math.max(0, oldalphai + oldalphaj - C);
                H = Math.min(0, oldalphai + oldalphaj);
            }

            double eta = 2 * productMatrix.get(i, j) - productMatrix.get(i, i) - productMatrix.get(j, j);
            double derivationForAlphaJ = yj*(Ei-Ej)/eta;
            double newAlphaj = oldalphaj-derivationForAlphaJ;
            if (newAlphaj<L) alpha.set(j,L);
            else if (newAlphaj>H) alpha.set(j,H);
            else alpha.set(j,newAlphaj);
            double aj = alpha.get(j).doubleValue();
            alpha.set(i,oldalphai + yi * yj * (oldalphaj - aj));
            double ai = alpha.get(i).doubleValue();
            double b1 = b - Ei - yi * (ai - oldalphai) * productMatrix.get(i, i) - yj * (aj - oldalphaj) * productMatrix.get(i, j);
            double b2 = b - Ej - yi * (ai - oldalphai) * productMatrix.get(i, j) - yj * (aj - oldalphaj) * productMatrix.get(j, j);

            if (0 < ai && ai < C)
                b = b1;
            else if (0 < aj && aj < C)
                b = b2;
            else
                b = (b1 + b2) / 2;
        }
    }

    private void Init() {
        //初始化alpha的权值，应该是全置0或者令alpha为0到C之间的某个随机数
        //Todo alpha参数还有很多优化可能
        alpha = new ArrayList<Double>(Collections.nCopies(data.getDataMatrix().getWidth(), new Double(0.0)));
        //初始化b的值，可以考虑随机或者置默认值
        //Todo 后续优化b参数取值
        b = 1;
        //求Kernel矩阵
        GetKernelMatrix();
        isInit=true;
        return ;
    }

    private void GetKernelMatrix() {
        Matrix x=data.getDataMatrix();
        int height = x.getHeight();
        productMatrix = new Matrix(new double[height][height]);
        for (int i=0;i<height;i++) {
            for (int j=0;j<height;j++) {
                double temp = fKernel.cal(x.get(i),x.get(j));
                productMatrix.set(i,j,temp);
            }
        }
    }

    private boolean convergence() {
        // 采用KKT条件判别是否收敛，在alpha维数过多的时候会过于缓慢
        /*
        * KKT条件如下：
        * yi*Ei >=0 when alpha = 0
        * yi*Ei ==0 when 0<alpha<C
        * yi*Ei <=0 when alpha = C
        */
        for (int i = 0;i<alpha.size();i++) {
            if ((data.getLabelMatrix().get(i,0)*calE(i)>=0 && Math.abs(alpha.get(i))<tol)
                ||(Math.abs(data.getLabelMatrix().get(i,0)*calE(i))<tol && alpha.get(i)>0 && alpha.get(i)-C < 0)
                ||(data.getLabelMatrix().get(i,0)*calE(i)>=0 && Math.abs(alpha.get(i)-C)<tol )) {
                continue;
            }
            else {
                return false;
            }
        }
        return true;
    }

    public int test(Matrix x) {
        // 在这里我发现，实际上还是要保留支持向量，因为测试的时候不可能单靠内积来进行计算。
        double f = 0.0;
        for (int i=0;i<alpha.size();i++) {
            f = f + alpha.get(i)*data.getLabelMatrix().get(i,1)*fKernel.cal(data.getDataMatrix().get(i),x);
        }
        f += b;
        if (f > 0) return 1;
        else return -1;
    }

    public KernelFunction getfKernel() {
        return fKernel;
    }

    public void setfKernel(KernelFunction fKernel) {
        this.fKernel = fKernel;
    }

    public double getB() {
        return b;
    }
}
