import java.util.*;

/** Independent small-input checks. Run after compiling src: javac -cp out -d out tools/SelfTest.java; java -cp out SelfTest. */
public class SelfTest {
    static final Random random = new Random(253730);
    static void require(boolean value) { if (!value) throw new AssertionError(); }
    static String dna(int n) {
        StringBuilder s = new StringBuilder();
        while (n-- > 0) s.append("ACGT".charAt(random.nextInt(4)));
        return s.toString();
    }
    static int distance(String a, String b) {
        if (a.isEmpty()) return b.length();
        if (b.isEmpty()) return a.length();
        return Math.min(distance(a.substring(1), b.substring(1)) + (a.charAt(0) == b.charAt(0) ? 0 : 1),
                1 + Math.min(distance(a.substring(1), b), distance(a, b.substring(1))));
    }
    static boolean equalWindow(String a, int i, String b, int j, int k) {
        for (int t = 0; t < k; t++) if (a.charAt(i+t) != b.charAt(j+t)) return false;
        return true;
    }
    static int lcs(String a, String b) {
        int best = 0;
        for (int mask = 0; mask < (1 << a.length()); mask++) {
            int j = 0, length = 0;
            boolean ok = true;
            for (int i = 0; i < a.length(); i++) if ((mask & (1 << i)) != 0) {
                while (j < b.length() && b.charAt(j) != a.charAt(i)) j++;
                if (j == b.length()) { ok = false; break; }
                j++; length++;
            }
            if (ok) best = Math.max(best, length);
        }
        return best;
    }
    public static void main(String[] args) {
        for (int run = 0; run < 500; run++) {
            String a = dna(random.nextInt(7)), b = dna(random.nextInt(7));
            require(Strains.editDistance(a,b) == distance(a,b));
            require(Strains.lcsLength(a,b) == lcs(a,b));
            int[] pi = Marker.prefixFunction(b);
            for (int i = 0; i < b.length(); i++) {
                int best = 0;
                for (int k = 1; k <= i; k++) if (equalWindow(b,0,b,i-k+1,k)) best = k;
                require(pi[i] == best);
            }
            List<Integer> hits = new ArrayList<>();
            for (int i = 0; i+b.length() <= a.length(); i++)
                if (equalWindow(a,i,b,0,b.length())) hits.add(i);
            require(Arrays.equals(Marker.findAll(a,b),hits.stream().mapToInt(Integer::intValue).toArray()));
            for (int k = 1; k <= 8; k++) {
                int shared = 0;
                for (int i = 0; i+k <= a.length(); i++) {
                    for (int j = 0; j+k <= b.length(); j++) if (equalWindow(a,i,b,j,k)) { shared++; break; }
                }
                require(Marker.sharedWords(a,b,k) == shared);
            }
        }
        require(Marker.sharedWords("AAAAAAAAAA","AAAA",3) == 8);
        // Distinct words with the same hash, 447592986; then a true match in that bucket.
        require(Marker.sharedWords("AGGAACGCATAA", "CGGTGATACAAT", 12) == 0);
        require(Marker.sharedWords("AGGAACGCATAA", "CGGTGATACAATAGGAACGCATAA", 12) == 1);
        for (int run = 0; run < 300; run++) {
            int n = 1+random.nextInt(11);
            int[] a = new int[n-1], b = new int[n-1], cost = new int[n];
            for (int i = 1; i < n; i++) { a[i-1] = i; b[i-1] = random.nextInt(i); }
            for (int i = 0; i < n; i++) cost[i] = random.nextInt(20);
            Input.Tree tree = new Input.Tree(n,a,b,cost);
            int[][] nb = tree.neighbours();
            boolean[] selected = Outbreak.cheapestTests(n,nb,cost);
            long actual = 0, best = Long.MAX_VALUE;
            for (int i = 0; i < n; i++) if (selected[i]) actual += cost[i];
            for (int i = 0; i < n-1; i++) require(selected[a[i]] || selected[b[i]]);
            for (int mask = 0; mask < (1 << n); mask++) {
                boolean ok = true; long sum = 0;
                for (int i = 0; i < n-1; i++) if ((mask & ((1<<a[i]) | (1<<b[i]))) == 0) { ok=false; break; }
                if (ok) {
                    for (int i=0;i<n;i++) if ((mask & (1<<i)) != 0) sum+=cost[i];
                    best=Math.min(best,sum);
                }
            }
            require(actual == best);
            int diameter=0;
            for (int root=0;root<n;root++) {
                int[] d=new int[n]; Arrays.fill(d,-1); d[root]=0;
                ArrayDeque<Integer> q=new ArrayDeque<>(); q.add(root);
                while (!q.isEmpty()) {
                    int v=q.remove();
                    for (int u:nb[v]) if (d[u]<0) { d[u]=d[v]+1; diameter=Math.max(diameter,d[u]); q.add(u); }
                }
            }
            require(Outbreak.longestChain(n,nb) == diameter);
        }
        require(Outbreak.longestChain(0,new int[0][]) == 0);
        require(Outbreak.cheapestTests(0,new int[0][],new int[0]).length == 0);
        System.out.println("PASS: 500 random string pairs, 300 brute-force tree checks, and boundary cases.");
    }
}
